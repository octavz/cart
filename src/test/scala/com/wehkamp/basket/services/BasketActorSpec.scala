package com.wehkamp.basket.services

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.TestActorRef
import com.wehkamp.basket.exceptions.{NotFoundException, StockException}
import com.wehkamp.basket.messages.{AddProduct, GetBasket, RemoveProduct}
import com.wehkamp.basket.models.{UserData, BasketItem, WProduct}
import com.wehkamp.basket.repositories.Repository
import org.scalatest.{Matchers, WordSpec}
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import akka.pattern.ask
import akka.util.Timeout
import com.wehkamp.basket.dtos.{BasketDTO, ProductDTO}

import scala.concurrent.duration._
import scala.concurrent.Await

class BasketActorSpec extends WordSpec with Matchers with MockitoSugar {
  implicit val actorSystem = ActorSystem()

  val uid = "userId"
  val pid = "productId"
  val basket = UserData(userId = "basketId", items = Set(BasketItem("pid1", 1)))
  val prods = Set(WProduct(pid, "name", "desc", 42, 10))
  implicit val akkaTimeout = Timeout(10.seconds)

  case class TestContext(actor: ActorRef, basket: BasketActor, repo: Repository)

  def service(repo: Repository = mock[Repository]): TestContext = {
    val ref = TestActorRef(Props(new BasketActor(repo)))
    TestContext(ref, ref.underlyingActor, repo)
  }

  "Authorize should call user function when the user is authorized" in {
    val context = service()
    when(context.repo.userExists(anyString)).thenReturn(true)

    val res = context.basket.authorizeAndCatchErrors(uid) {
      ServiceResponse("test")
    }
    verify(context.repo).userExists(uid)
    res.entity should contain("test")
  }

  "Authorize should not call user function when the user is not authorized" in {
    val context = service()
    when(context.repo.userExists(anyString)).thenReturn(false)

    val res = context.basket.authorizeAndCatchErrors(uid) {
      assert(false)
      ServiceResponse("test")
    }
    verify(context.repo).userExists(uid)
    res.entity shouldBe empty
    res.errCode should contain(401)
    res.errMessage should contain("Not authorized")
  }

  "Authorize should return 404 when user function fails with NotFoundException" in {
    val context = service()
    when(context.repo.userExists(anyString)).thenReturn(true)

    val res = context.basket.authorizeAndCatchErrors(uid) {
      throw new NotFoundException("test")
    }
    verify(context.repo).userExists(uid)
    res.entity shouldBe empty
    res.errCode should contain(404)
    res.errMessage should contain("test")
  }

  "Authorize should return 422 when user function fails with StockExeception" in {
    val context = service()
    when(context.repo.userExists(anyString)).thenReturn(true)

    val res = context.basket.authorizeAndCatchErrors(uid) {
      throw new StockException("test")
    }
    verify(context.repo).userExists(uid)
    res.entity shouldBe empty
    res.errCode should contain(422)
    res.errMessage should contain("test")
  }

  "Authorize should return 500 when user function fails with unknown exception" in {
    val context = service()
    when(context.repo.userExists(anyString)).thenReturn(true)

    val res = context.basket.authorizeAndCatchErrors(uid) {
      throw new RuntimeException("test")
    }
    verify(context.repo).userExists(uid)
    res.entity shouldBe empty
    res.errCode should contain(500)
    res.errMessage should contain("test")
  }

  "Validate should correctly validate when product exists" in {
    val context = service()
    when(context.repo.productExists(anyString)).thenReturn(true)
    val res = context.basket.validateProductId(pid) {
      ServiceResponse("test")
    }
    verify(context.repo).productExists(pid)
    res.entity should contain("test")
  }

  "Validate should correctly validate when product does not exists" in {
    val context = service()
    when(context.repo.productExists(anyString)).thenReturn(false)
    val res = context.basket.validateProductId(pid) {
      assert(false)
      ServiceResponse("test")
    }
    verify(context.repo).productExists(pid)
    res.entity shouldBe empty
    res.errCode should contain(404)
    res.errMessage should contain("No product found")
  }

  "getBasketByUserId should return existing basket if there is one" in {
    val context = service()

    when(context.repo.userExists(anyString)).thenReturn(true)
    when(context.repo.getProductsById(any[Set[String]])).thenReturn(prods)
    when(context.repo.getBasketByUserId(anyString)).thenReturn(Some(basket))

    val res = Await.result(context.actor ? GetBasket(uid), 10.second).asInstanceOf[ServiceResponse[BasketDTO]]

    verify(context.repo).userExists(uid)
    verify(context.repo).getBasketByUserId(uid)
    verify(context.repo).getProductsById(basket.items.map(_.productId))

    res.entity should not be empty
    val ent = res.entity.get
    ent.content should contain(basket.items.head)
    ent.products should contain(ProductDTO(prods.head.id, prods.head.name, prods.head.description, prods.head.price))

    res.errCode shouldBe empty
  }

  "getBasketByUserId should return new basket if it doesn't exists" in {
    val context = service()

    when(context.repo.userExists(anyString)).thenReturn(true)
    when(context.repo.getProductsById(any[Set[String]])).thenReturn(prods)
    when(context.repo.getBasketByUserId(anyString)).thenReturn(None)
    when(context.repo.persistBasket(anyString, any[UserData])).thenReturn(basket)

    val res = Await.result(context.actor ? GetBasket(uid), 10.seconds).asInstanceOf[ServiceResponse[BasketDTO]]

    verify(context.repo).userExists(uid)
    verify(context.repo).getBasketByUserId(uid)
    verify(context.repo).persistBasket(uid, UserData(uid))

    res.entity.get.content shouldBe basket.items
    res.errCode shouldBe empty
  }

  "addProduct should add a product to basket when it doesn't belong to the basket" in {
    val context = service()
    val b = UserData(uid, Set.empty)
    val item = BasketItem("pid1", 2)

    when(context.repo.userExists(anyString)).thenReturn(true)
    when(context.repo.productExists(anyString)).thenReturn(true)
    when(context.repo.getBasketByUserId(anyString)).thenReturn(Some(b))
    when(context.repo.getProductsById(any[Set[String]])).thenReturn(prods)

    when(context.repo.decreaseStock(any[BasketItem])).thenReturn(9)

    when(context.repo.persistBasket(anyString, any[UserData])).thenAnswer(new Answer[UserData] {
      override def answer(invocation: InvocationOnMock) =
        invocation.getArguments.last.asInstanceOf[UserData]
    })

    val res = Await.result(context.actor ? AddProduct(uid, item), 10.second).asInstanceOf[ServiceResponse[BasketDTO]]

    verify(context.repo).userExists(uid)
    verify(context.repo).productExists("pid1")
    verify(context.repo).getBasketByUserId(uid)
    verify(context.repo).persistBasket(uid, b.copy(items = Set(BasketItem("pid1", 2))))

    res.entity should not be empty
    res.entity.get.content.size shouldBe 1
    res.entity.get.content should contain(BasketItem("pid1", 2))
    res.errCode shouldBe empty
  }

  "addProduct should add a product to basket and increase quantity when id already belongs to basket" in {
    val context = service()
    val item = BasketItem("pid1", 2)

    when(context.repo.userExists(anyString)).thenReturn(true)
    when(context.repo.productExists(anyString)).thenReturn(true)
    when(context.repo.getBasketByUserId(anyString)).thenReturn(Some(basket))
    when(context.repo.getProductsById(any[Set[String]])).thenReturn(prods)
    when(context.repo.decreaseStock(any[BasketItem])).thenReturn(9)
    when(context.repo.persistBasket(anyString, any[UserData])).thenAnswer(new Answer[UserData] {
      override def answer(invocation: InvocationOnMock) =
        invocation.getArguments.last.asInstanceOf[UserData]
    })

    val res = Await.result(context.actor ? AddProduct(uid, item), 10.second).asInstanceOf[ServiceResponse[BasketDTO]]

    verify(context.repo).userExists(uid)
    verify(context.repo).productExists("pid1")
    verify(context.repo).getBasketByUserId(uid)
    verify(context.repo).persistBasket(uid, basket.copy(items = Set(BasketItem("pid1", 3))))

    res.entity should not be empty
    res.entity.get.content.size shouldBe 1
    res.entity.get.content should contain(BasketItem("pid1", 3))
    res.errCode shouldBe empty
  }

  "removeProduct should fail with 404 when no product exists in basket" in {
    val context = service()
    val b = UserData(uid, Set(BasketItem("p1", 1), BasketItem("p2", 1), BasketItem("p3", 1)))

    when(context.repo.userExists(anyString)).thenReturn(true)
    when(context.repo.productExists(anyString)).thenReturn(true)
    when(context.repo.getProductsById(any[Set[String]])).thenReturn(prods)
    when(context.repo.getBasketByUserId(anyString)).thenReturn(Some(b))
    when(context.repo.increaseStock(anyString)).thenReturn(9)

    val res = Await.result(context.actor ? RemoveProduct(uid, "p4"), 10.seconds).asInstanceOf[ServiceResponse[BasketDTO]]

    val newbasket = b.copy(items = Set(BasketItem("p1", 1), BasketItem("p3", 1)))
    verify(context.repo).userExists(uid)
    verify(context.repo).productExists("p4")
    verify(context.repo).getBasketByUserId(uid)
    verify(context.repo, times(0)).persistBasket(uid, newbasket)

    res.entity shouldBe empty
    res.errCode should contain(404)
  }

  "removeProduct should return basket without this product" in {
    val context = service()
    val b = UserData("bid", Set(BasketItem("p1", 1), BasketItem("p2", 1), BasketItem("p3", 1)))

    when(context.repo.userExists(anyString)).thenReturn(true)
    when(context.repo.productExists(anyString)).thenReturn(true)
    when(context.repo.getProductsById(any[Set[String]])).thenReturn(prods)
    when(context.repo.getBasketByUserId(anyString)).thenReturn(Some(b))
    when(context.repo.increaseStock(anyString)).thenReturn(9)

    when(context.repo.persistBasket(anyString, any[UserData])).thenAnswer(new Answer[UserData] {
      override def answer(invocation: InvocationOnMock) =
        invocation.getArguments.last.asInstanceOf[UserData]
    })

    val res = Await.result(context.actor ? RemoveProduct(uid, "p2"), 10.seconds).asInstanceOf[ServiceResponse[BasketDTO]]

    val newbasket = b.copy(items = Set(BasketItem("p1", 1), BasketItem("p3", 1)))
    verify(context.repo).userExists(uid)
    verify(context.repo).productExists("p2")
    verify(context.repo).getBasketByUserId(uid)
    verify(context.repo).persistBasket(uid, newbasket)

    res.entity should not be empty
    res.entity.get.content.size shouldBe 2
    res.entity.get.content should contain allOf(newbasket.items.head, newbasket.items.last)
    res.errCode shouldBe empty
  }

  "When removing from basket should verify existence in basket" in {
    intercept[NotFoundException] {
      basket.remove("p")
    }
  }

  "When removing from basket should decrement if product exists" in {
    val b = UserData(uid, Set(BasketItem(pid, 4)))
    val newBasket = b.remove(pid)
    newBasket.items.head.quantity shouldBe 3
  }


}
