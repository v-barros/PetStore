package scala.objects

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.util.Random

object PetStore {

  private val minPhoneRand = 100000000L
  private val maxPhoneRand = 999999999L
  private val minZipRand = 100000000L
  private val maxZipRand = 999999999L
  private val feederDirectory = "feeders/"
  private var registeredUser="bTxccKkQ2x"

  /* ---- feeders ---- */

  private val username = Iterator.continually(Map("username"->s"${Random.alphanumeric.take(10).mkString}"))
  private val password = Iterator.continually(Map("password"->"123456"))
  private val firstName = Iterator.continually(Map("firstName"->s"${Random.alphanumeric.take(10).mkString}"))
  private val lastName = Iterator.continually(Map("lastName"->s"${Random.alphanumeric.take(10).mkString}"))
  private val email = Iterator.continually(Map("email"->s"${Random.alphanumeric.take(10).mkString}@perftest.com"))
  private val phone = Iterator.continually(Map("phone"->(Random.nextLong(maxPhoneRand-minPhoneRand)+minPhoneRand)))
  private val addressFeeder = csv(feederDirectory + "addresses.csv").circular
  private val state = Iterator.continually(Map("state"->"New Jersey"))
  private val city = Iterator.continually(Map("city"->"test"))
  private val zip = Iterator.continually(Map("zip" -> (Random.nextLong(maxZipRand - minZipRand) + minZipRand)))
  private val country = Iterator.continually(Map("country" -> "Testland"))
  private val languagePreference = Iterator.continually(Map("languagePreference"->"english"))
  private val categoryFeeder = csv(feederDirectory + "animalsCategories.csv").random
  private val listOption = Array(Map("listOption" -> "true"), Map("listOption" -> "false")).random
  private val bannerOption = Iterator.continually(Map("bannerOption" -> "true"))
  private val newAccount = Iterator.continually(Map("newAccount" -> "Save Account Information"))

  /* ----- Headers ----*/
  private val headersNewUser = Map(
    "Upgrade-Insecure-Requests"->"1",
    "Sec-Fetch-Dest"-> "document",
    "Sec-Fetch-Mode"-> "navigate",
    "Sec-Fetch-Site"-> "same-origin",
    "Sec-Fetch-User"-> "?1",
    "Pragma"-> "no-cache",
    "Cache-Control"-> "no-cache",
    "TE"-> "trailers",
    "Accept"-> "text/html,application/xhtml+xml,application/xm;q=0.9,image/avif,image/webp,*;q=0.8",
    "Accept-Language"-> "en-US,en;q=0.5",
    "Accept-Encoding"-> "gzip, deflate, br")

  def HomePage ={
    exec{
      http("GET - Home page")
        .get("/")
    }
  }

  def EnterStore = {
    exec {
      http("GET - Enter Store")
        .get("/actions/Catalog.action")
        .check(regex(";categoryId=(\\w+)\"").findRandom.saveAs("categoryId"))
    }
    .exec(getCookieValue(CookieKey("JSESSIONID")))
  }

  def LoginPage ={
    exec{
      http("GET - Login Page")
        .get("/actions/Account.action;jsessionid=${JSESSIONID}?signonForm=")
        .check(regex("\"_sourcePage\"\\svalue=\"(.+?)\"").find(1).saveAs("sourcePage"))
        .check(regex("\"__fp\"\\svalue=\"(.+?)\"").find(1).saveAs("fp"))
    }
  }

  def Login = {
    feed(password)
    .exec{
      session =>
        val newSession = session.set("registeredUser",registeredUser)
        newSession
    }
    .exec{
      http("ṔOST - Login")
        .post("/actions/Account.action")
        .formParamMap(Map(
          "username"->"${registeredUser}",
          "password"->"${password}",
          "signon"->"Login",
          "_sourcePage"->"${sourcePage}",
          "__fp" ->"${fp}"))
    }
  }

  def RegisterPage = {
    exec {
      http("GET - Register Page")
        .get("/actions/Account.action?newAccountForm=")
        .check(regex("\"_sourcePage\"\\svalue=\"(.+?)\"").find(1).saveAs("sourcePage"))
        .check(regex("\"__fp\"\\svalue=\"(.+?)\"").find(1).saveAs("fp"))
    }
  }

  def NewUser = {
    feed(addressFeeder).feed(categoryFeeder).feed(username).feed(password).feed(firstName).feed(lastName).feed(email).feed(phone)
      .feed(city).feed(state).feed(zip).feed(country).feed(languagePreference).feed(listOption).feed(bannerOption).feed(newAccount)
      .exec{
      http("POST - New User")
        .post("/actions/Account.action?" +
          "account.lastName=${lastName}" +
          "&account.city=${city}" +
          "&_sourcePage=${sourcePage}" +
          "&account.state=${state}" +
          "&account.phone=${phone}" +
          "&account.languagePreference=${languagePreference}" +
          "&account.firstName=${firstName}" +
          "&account.zip=${zip}" +
          "&account.country=${country}" +
          "&password=${password}" +
          "&username=${username}" +
          "&newAccount=${newAccount}" +
          "&account.listOption=${listOption}" +
          "&account.bannerOption=${bannerOption}" +
          "&repeatedPassword=${password}" +
          "&account.address2=${address2}" +
          "&__fp=${fp}" +
          "&account.favouriteCategoryId=${category}" +
          "&account.email=${email}" +
          "&account.address1=${address1}")
        .headers(headersNewUser)
        .check(regex(";categoryId=(\\w+)\"").findRandom.saveAs("categoryId"))
    }
      .exec{
        session =>
          // Update Registered User for other scenarios
          registeredUser = session("username").as[String]
          session
      }
  }

  def CategoryPage ={
    exec{
      http("GET - Category Page")
      .get("/actions/Catalog.action?viewCategory=&categoryId=${categoryId}")
        .check(regex(";productId=([\\w\\d-]+)\"").findRandom.saveAs("productId"))
    }
  }

  def ProductPage ={
    exec{
      http("GET - Product Page")
        .get("/actions/Catalog.action?viewProduct=&productId=${productId}")
        .check(regex(";itemId=([\\w\\d-]+)\"").findRandom.saveAs("itemId"))
    }
  }

  def AddToCart = {
    exec {
      http("GET - Add Item To Cart")
        .get("/actions/Cart.action?addItemToCart=&workingItemId=${itemId}")
    }
  }

  def UpdateCart = {
    exec {
      http("POST - Update Cart")
        .post("/actions/Cart.action")
        .formParamMap(Map(
          "${itemId}"             ->s"${Random.nextLong(5) + 2}",
          "updateCartQuantities"  ->"Update Cart",
          "_sourcePage"           ->"${sourcePage}",
          "__fp"                  ->"${fp}"
        ))
    }
  }

  def ProceedToCheckout = {
    exec{
      http("GET - Proceed To Checkout")
        .get("/actions/Order.action?newOrderForm=")
        .check(regex("order\\.billToFirstName\".+?value=\"([\\d\\w]+)\"").find(0).saveAs("billToFirstName"))
        .check(regex("order\\.billToLastName\".+?value=\"([\\d\\w]+)\"").find(0).saveAs("billToLastName"))
        .check(regex("order\\.billAddress1\".+?value=\"([\\d\\w ]+)\"").find(0).saveAs("billAddress1"))
        .check(regex("order\\.billAddress2\".+?value=\"([\\d\\w ]+)\"").find(0).saveAs("billAddress2"))
        .check(regex("order\\.billCity\".+?value=\"([\\w \\d]+)\"").find(0).saveAs("billCity"))
        .check(regex("order\\.billCountry\".+?value=\"([\\w ]+)\"").find(0).saveAs("billCountry"))
        .check(regex("order\\.billState\".+?value=\"([\\w ]+)\"").find(0).saveAs("billState"))
        .check(regex("order\\.billZip\".+?value=\"([\\d ]+)\"").find(0).saveAs("billZip"))
        .check(regex("\"_sourcePage\"\\svalue=\"(.+?)\"").find(1).saveAs("sourcePage"))
        .check(regex("\"__fp\"\\svalue=\"(.+?)\"").find(1).saveAs("fp"))
    }
      .exec{ session =>
        val newSession  = session.set("creditCard","999 9999 9999 9999")
        val newSession1 = newSession.set("cardType","Visa")
        val newSession2 = newSession1.set("expiryDate","12/03")
        newSession2
      }
  }

  def ConfirmationPage = {
    exec {
      http("POST - Confirmation Page")
        .post("/actions/Order.action")
        .formParamMap(Map(
            "order.cardType"        ->"${cardType}",
            "order.creditCard"      ->"${creditCard}",
            "order.expiryDate"      ->"${expiryDate}",
            "order.billToFirstName" ->"${billToFirstName}",
            "order.billToLastName"  ->"${billToLastName}",
            "order.billAddress1"    ->"${billAddress1}",
            "order.billAddress2"    ->"${billAddress2}",
            "order.billCity"        ->"${billCity}",
            "order.billState"       ->"${billState}",
            "order.billZip"         ->"${billZip}",
            "order.billCountry"     ->"${billCountry}",
            "newOrder"              ->"Continue",
            "_sourcePage"           ->"${sourcePage}",
            "__fp"                  ->"${fp}"
        ))
    }
  }

  def ConfirmPurchaseOrder = {
    exec{
      http("GET - Confirm Purchase Order")
        .get("/actions/Order.action?newOrder=&confirmed=true")
    }
  }
}
