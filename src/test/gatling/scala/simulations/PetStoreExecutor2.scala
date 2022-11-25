package scala.simulations

import io.gatling.core.Predef.{Simulation, closedInjectionProfileFactory, configuration, constantConcurrentUsers, incrementUsersPerSec, openInjectionProfileFactory, rampUsers, scenario}
import io.gatling.http.Predef._

import scala.concurrent.duration.DurationInt
import scala.objects.PetStore

class PetStoreExecutor2 extends Simulation{
  private val petStoreHost = "https://petstore.octoperf.com"

  val httpConfiguration = http.baseUrl(petStoreHost)

  def scnNewUser = scenario("New User")
    .pace(1.seconds)
    .exec(PetStore.HomePage)
    .exec(PetStore.EnterStore)
    .exec(PetStore.LoginPage)
    .exec(PetStore.RegisterPage)
    .exec(PetStore.NewUser)

  def scnCreateOrder = scenario("Create Order")
    .pace(1.seconds)
    .exec(PetStore.HomePage)
    .exec(PetStore.EnterStore)
    .exec(PetStore.LoginPage)
    .exec(PetStore.Login)
    .exec(PetStore.CategoryPage)
    .exec(PetStore.ProductPage)
    .exec(PetStore.AddToCart)
    .exec(PetStore.ProceedToCheckout)
    .exec(PetStore.ConfirmationPage)
    .exec(PetStore.ConfirmPurchaseOrder)

  def scnCreateOrderMany = scenario ("Create Order Many")
    .pace(1.seconds)
    .exec(PetStore.HomePage)
    .exec(PetStore.EnterStore)
    .exec(PetStore.LoginPage)
    .exec(PetStore.Login)
    //*-- for some reason, the 'repeat()' method is not working --*/
    .exec(PetStore.EnterStore)
    .exec(PetStore.CategoryPage)
    .exec(PetStore.ProductPage)
    .exec(PetStore.AddToCart)
    .exec(PetStore.EnterStore)
    .exec(PetStore.CategoryPage)
    .exec(PetStore.ProductPage)
    .exec(PetStore.AddToCart)
    .exec(PetStore.EnterStore)
    .exec(PetStore.CategoryPage)
    .exec(PetStore.ProductPage)
    .exec(PetStore.AddToCart)
    .exec(PetStore.UpdateCart)
    .exec(PetStore.ProceedToCheckout)
    .exec(PetStore.ConfirmationPage)
    .exec(PetStore.ConfirmPurchaseOrder)

  setUp(
    /* group() is not working too, but it would be good to determine the performance for different scenarios, once they use the same request and their stats are mixed on the report*/

    scnNewUser.inject(
      //* Avg. load*/
      rampUsers(80).during(8.minutes)
    ),
    scnCreateOrder.inject(
      /* Peak Load - 4x avg load*/
      rampUsers(60).during(6.minute),
      rampUsers(80).during(120.seconds),
    ),
    scnCreateOrderMany.inject(
      /* Stress Load - 2x avg load*/
      rampUsers(40).during(4.minutes),
      rampUsers(80).during(4.minutes),
    )

  ).protocols(httpConfiguration)
}
