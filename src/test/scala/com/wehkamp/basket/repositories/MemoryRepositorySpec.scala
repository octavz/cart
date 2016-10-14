package com.wehkamp.basket.repositories

import akka.actor.ActorSystem
import com.wehkamp.basket.exceptions.{NotFoundException, StockException}
import com.wehkamp.basket.models.{UserData, BasketItem}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Matchers, WordSpec}

class MemoryRepositorySpec extends WordSpec with Matchers with MockitoSugar {
  implicit val actorSystem = ActorSystem()

  val uid = "a"
  val pid = "id_1"
  val basket = UserData(userId = "basketId", items = Set(BasketItem(pid, 2)))

  def newRepo = new MemoryRepository

  "userExists() should return true when user exists" in {
    newRepo.userExists(uid) shouldBe true
  }

  "userExists() should return false when user doesn't exists" in {
    newRepo.userExists("x") shouldBe false
  }

  "productExist() should return true when product exists" in {
    newRepo.productExists(pid) shouldBe true
  }

  "productExists() should return false when product doesn't exists" in {
    newRepo.productExists("x") shouldBe false
  }

  "getBasketByUserId() should return Some basket when it exists and add products" in {
    val repo = newRepo
    repo.basketStore.put(uid, UserData("bid"))
    val basket = repo.getBasketByUserId(uid)
    basket should contain(UserData("bid"))
  }

  "getBasketByUserId() should return None basket when it doesn't exists" in {
    val basket = newRepo.getBasketByUserId(uid)
    basket shouldBe empty
  }

  "decreaseStock should correctly decrement stock" in {
    val repo = newRepo
    val res = repo.decreaseStock(BasketItem(pid, 2))
    res shouldBe repo.maxStock - 2
    repo.catalog("id_1").stock shouldBe repo.maxStock - 2
  }

  "decreaseStock should throw StockException when stock is empty" in {
    val repo = newRepo
    repo.catalog(pid).stock = 0
    intercept[StockException] {
      repo.decreaseStock(BasketItem(pid, 1))
    }
  }

  "decreaseStock should throw StockException when not enough stock" in {
    val repo = newRepo
    intercept[StockException] {
      repo.decreaseStock(BasketItem(pid, repo.maxStock + 1))
    }
  }

  "decreaseStock should throw NotFoundException when product not found in stock" in {
    val repo = newRepo
    intercept[NotFoundException] {
      repo.decreaseStock(BasketItem("x", 1))
    }
  }

  "increaseStock should increment the stock with 1" in {
    val repo = newRepo
    val res = repo.increaseStock(pid)
    res shouldBe repo.maxStock + 1
    repo.catalog("id_1").stock shouldBe repo.maxStock + 1
  }

  "increaseStock should throw NotFoundException when product not found in stock" in {
    val repo = newRepo
    intercept[NotFoundException] {
      repo.increaseStock("x")
    }
  }

  "persistBasket should add the basket in store" in {
    val repo = newRepo
    repo.persistBasket(uid, basket)
    val res = repo.basketStore.get(uid)
    res should contain(basket)
  }

  "persistBasket should replace the basket in store when it exists already" in {
    val repo = newRepo
    repo.basketStore(pid) = basket
    val newBasket = basket.copy(userId = "another")
    repo.persistBasket(uid, newBasket)
    val res = repo.basketStore.get(uid)
    res should contain(newBasket)
  }

  "persistBasket should throw NotFoundException when user not found " in {
    val repo = newRepo
    intercept[NotFoundException] {
      repo.persistBasket("x", basket)
    }
  }

  "getProductById should return Some when it exists" in {
    val repo = newRepo
    val res = repo.getProductsById(Set("id_1", "id_2", "x"))
    res.size shouldBe 2
    res.find(_.id == pid).get.name shouldBe "name 1"
  }

}
