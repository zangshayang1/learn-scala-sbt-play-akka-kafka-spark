package controllers

import security.UserAuthAction
import play.api.mvc.Controller
import services.{TagEventProducer, ReadService}
import play.api.libs.json.Json

/** Design a controller bridging user actions with services.

  user authentication --- UserAuthAction
  user create/delete tags --- TagEventProducer
  user receive tags --- ReadService

*/
class TagController(tagEventProducer: TagEventProducer,
                    userAuthAction: UserAuthAction,
                    readService: ReadService) extends Controller {

  // define two case class to store info extracted from user requests
  import java.util.UUID
  case class CreateTagData(text: String)
  case class DeleteTagData(id: UUID)

  // define forms
  import play.api.data.Form
  import play.api.data.Forms._
  val createTagForm = Form {
    mapping(
      "text" -> nonEmptyText
    )(CreateTagData.apply)(CreateTagData.unapply)
  }

  val deleteTagForm = Form {
    mapping(
      "id" -> uuid
    )(DeleteTagData.apply)(DeleteTagData.unapply)
  }

  /** Define the actual createTag/deleteTag method exposed via an HTTP endpoint

    Since tagEventProducer.createTag() returns Future[Seq[Tag]]
      the entire method is async
      Consequently, the BadRequest is wrapped with Future[]
  */

  def createTag() = userAuthAction { implicit request =>
    createTagForm.bindFromRequest.fold(
      formWithErrors => BadRequest,
      userData => {
        tagEventProducer.createTag(userData.text, request.user.userId)
        Ok
      }
    )
  }

  def deleteTag() = userAuthAction { implicit request =>
    deleteTagForm.bindFromRequest.fold(
      formWithErrors => BadRequest,
      userData => {
        tagEventProducer.deleteTag(userData.id, request.user.userId)
        Ok
      }
    )
  }

  /** Define the action that shows all the tags without making changing
    using regular Action instead of UserAuthAction because unauthorized users can query tags as well
  */
  import play.api.mvc.Action
  import scala.concurrent.ExecutionContext.Implicits.global // !@ Implicits @! NOT implicits, NOT Implicit ...
  def getTags = Action.async { implicit request =>
    val tagsF = readService.getTags
    tagsF.map { tags => Ok(Json.toJson(tags)) }
  }
}
