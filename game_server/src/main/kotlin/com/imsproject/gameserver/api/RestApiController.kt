package com.imsproject.gameserver.api

import com.imsproject.common.dataAccess.DaoException
import com.imsproject.common.gameserver.GameRequest
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.common.utils.Response
import com.imsproject.gameserver.dataAccess.DAOController
import com.imsproject.gameserver.toResponseEntity
import com.imsproject.common.utils.fromJson
import com.imsproject.gameserver.business.GameRequestFacade
import com.imsproject.gameserver.business.ParticipantService
import com.imsproject.gameserver.business.auth.AuthService
import com.imsproject.gameserver.business.auth.Credentials
import com.imsproject.gameserver.dataAccess.implementations.ParticipantsDAO
import com.imsproject.gameserver.dataAccess.models.*
import com.imsproject.gameserver.runTimed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import org.slf4j.LoggerFactory
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.core.io.ResourceLoader
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.web.bind.annotation.*
import java.util.*
import java.util.concurrent.Executors

@EnableMethodSecurity
@RestController
class RestApiController(
    private val gameRequestFacade: GameRequestFacade,
    private val authService: AuthService,
    private val participantService: ParticipantService,
    private val daoController: DAOController,
    private val resources: ResourceLoader,
    private val participantsDAO: ParticipantsDAO
) : ErrorController {

    @RequestMapping("/login", method = [RequestMethod.POST, RequestMethod.GET])
    fun login(@RequestHeader(value = "Authorization") header : String?): ResponseEntity<String> {
        if(header == null){
            return Response.getError("No Authorization header").toResponseEntity(HttpStatus.BAD_REQUEST)
        }
        // base64 encoded user:password
        val credentials = header.removePrefix("Basic ")
        val decoded = String(Base64.getDecoder().decode(credentials))
        val (userId, password) = decoded.split(":")
        return authService.authenticateUser(userId, password).toResponseEntity()
    }

    @PostMapping("/manager")
    fun manager(@RequestBody body : String): ResponseEntity<String> {
        return withParsedBody<GameRequest>(body) { request ->
            withErrorHandling("Error handling game request") {
                gameRequestFacade.handleGameRequest(request).toResponseEntity()
            }
        }
    }

    // =========================================================================== |
    // ============================= ADMIN ENDPOINTS ============================= |
    // =========================================================================== |

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/operators/{action}")
    fun operators(@PathVariable action: String, @RequestBody body : String): ResponseEntity<String> {
        return withParsedBody<Credentials>(body) { credentials ->
            withErrorHandling("Error handling operator request") {
                try {
                    when (action) {
                        "add" -> authService.createUser(credentials)
                        "remove" -> authService.deleteUser(credentials.userId)
                        "get" -> {
                            val users = authService.getAllUsers()
                            return Response.getOk(users).toResponseEntity()
                        }

                        else -> Response.getError("Invalid action").toResponseEntity(HttpStatus.BAD_REQUEST)
                    }
                    Response.getOk().toResponseEntity()
                } catch (e: IllegalArgumentException) {
                    return Response.getError(e).toResponseEntity(HttpStatus.BAD_REQUEST)
                }
            }
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/participants/{action}")
    fun participants(@PathVariable action: String, @RequestBody body : String): ResponseEntity<String> {
        return withParsedBody<ParticipantDTO>(body) { participant ->
            withErrorHandling("Error handling participant request") {
                try{
                    when(action){
                        "add" -> {
                            log.debug("Adding participant: {}", participant)
                            val id = participantService.addParticipant(participant)
                            log.debug("Successfully Added participant with id: {}", id)
                            Response.getOk(id).toResponseEntity()
                        }
                        "remove" ->{
                            log.debug("Removing participant: {}", participant)
                            val pid = participant.pid ?: throw IllegalArgumentException("Participant id not provided")
                            participantService.remove(pid)
                            log.debug("Successfully removed participant with id: {}", pid)
                            Response.getOk().toResponseEntity()
                        }
                        "get" -> {
                            val participants = participantService.getAll()
                            Response.getOk(participants).toResponseEntity()
                        }
                        else -> Response.getError("Invalid action").toResponseEntity(HttpStatus.BAD_REQUEST)
                    }
                } catch (e: IllegalArgumentException) {
                    log.debug("Error handling participant request", e)
                    Response.getError(e).toResponseEntity(HttpStatus.BAD_REQUEST)
                }
            }
        }
    }

    // =========================================================================== |
    // ============================ DATA ENDPOINTS =============================== |
    // =========================================================================== |

    @PostMapping("/data/participant/select")
    fun dataSelectParticipants(@RequestBody body: String): ResponseEntity<String> {
        return withParsedBody<ParticipantDTO>(body) { participantDTO ->
            withErrorHandling("Error selecting participants"){
                if(participantDTO.pid == null) {
                    Response.getOk(daoController.handleSelectAllParticipants()).toResponseEntity()
                } else {
                    Response.getOk(daoController.handleSelect(participantDTO)).toResponseEntity()
                }
            }
        }
    }

    // ===== Experiment ===== |

    @RequestMapping("/data/experiment/select/names", method = [RequestMethod.POST, RequestMethod.GET])
    fun dataSelectExperiments(): ResponseEntity<String> {
        return withErrorHandling("Error selecting experiments"){
            Response.getOk(daoController.handleSelectAllExperimentsWithNames()).toResponseEntity()
        }
    }

    // todo: delete
    @PostMapping("/data/experiment/{action}")
    fun dataTest2(
        @PathVariable action: String,
        @RequestBody body: String): ResponseEntity<String> {
        var obj = fromJson<ExperimentDTO>(body)

        when(action) {
            "insert" -> { daoController.handleInsert(obj) }
            "delete" -> { daoController.handleDelete(obj) }
            "update" -> { daoController.handleUpdate(obj) }
            "select" -> { daoController.handleSelect(obj) }
        }
        return Response.getOk().toResponseEntity()
    }


    private data class QnA(val question: String, val answer: String)
    private data class ExperimentFeedback(val expId: Int, val pid: Int, val qnas: List<QnA>)
    @PostMapping("/data/experiment/insert/feedback")
    fun dataInsertExperimentFeedback(@RequestBody body: String): ResponseEntity<String> {
        return withParsedBody<ExperimentFeedback>(body){ feedback ->
            withErrorHandling("Error inserting experiment feedback"){
                val feedbackDTOs = feedback.qnas.map { qna ->
                    ExperimentFeedbackDTO(
                        expId = feedback.expId,
                        pid = feedback.pid,
                        question = qna.question,
                        answer = qna.answer
                    )
                }

                return try{
                    val (_,runTime) = runTimed { daoController.handleBulkInsertExperimentFeedback(feedbackDTOs) }
                    log.debug("Inserted {} feedback entries in {}ms for expId {}", feedbackDTOs.size, runTime , feedback.expId)
                    Response.getOk().toResponseEntity()
                } catch(e: DaoException){
                    log.error("Error inserting experiment feedback - Feedback already submitted", e)
                    Response.getError("Feedback already submitted").toResponseEntity(HttpStatus.BAD_REQUEST)
                }
            }
        }
    }

    @PostMapping("/data/experiment/select/feedback")
    fun dataSelectExperimentsFeedback(@RequestBody body: String): ResponseEntity<String> {
        return withParsedBody<ExperimentFeedbackDTO>(body){ experimentFeedbackDTO ->
            withErrorHandling("Error selecting experiment feedback") {
                Response.getOk(daoController.handleSelectListExperimentsFeedback(experimentFeedbackDTO)).toResponseEntity()
            }
        }
    }

    // ===== Session ===== |

    @PostMapping("/data/session/select")
    fun dataSelectSessions(@RequestBody body: String): ResponseEntity<String> {
        return withParsedBody<SessionDTO>(body) { sessionDTO ->
            withErrorHandling("Error selecting sessions"){
                if(sessionDTO.sessionId != null) {
                    Response.getOk(daoController.handleSelect(sessionDTO)).toResponseEntity()
                } else {
                    Response.getOk(daoController.handleSelectListSessions(sessionDTO)).toResponseEntity()
                }
            }
        }
    }

    private data class SessionFeedback(val sessionId: Int, val pid: Int, val qnas: List<QnA>)
    @PostMapping("/data/session/insert/feedback")
    fun dataInsertSessionFeedback(@RequestBody body: String): ResponseEntity<String> {
        return withParsedBody<SessionFeedback>(body) { feedback ->
            withErrorHandling("Error inserting session feedback") {
                val feedbackDTOs = feedback.qnas.map {
                    SessionFeedbackDTO(
                        sessionId = feedback.sessionId,
                        pid = feedback.pid,
                        question = it.question,
                        answer = it.answer
                    )
                }
                try{
                    val (_, runTime) = runTimed { daoController.handleBulkInsertSessionFeedback(feedbackDTOs) }
                    log.debug("Inserted {} feedback entries in {}ms for sessionId {}", feedbackDTOs.size, runTime, feedback.sessionId)
                    Response.getOk().toResponseEntity()
                } catch(e: DaoException){
                    log.error("Error inserting session feedback - Feedback already submitted", e)
                    Response.getError("Feedback already submitted").toResponseEntity(HttpStatus.BAD_REQUEST)
                }
            }
        }
    }

    @PostMapping("/data/session/select/feedback")
    fun dataSelectSessionsFeedback(@RequestBody body: String): ResponseEntity<String> {
        return withParsedBody<SessionFeedbackDTO>(body){ sessionFeedbackDTO ->
            withErrorHandling("Error selecting session feedback") {
                Response.getOk(daoController.handleSelectListSessionsFeedback(sessionFeedbackDTO)).toResponseEntity()
            }
        }
    }

    private data class Events(val sessionId: Int, val events: List<String>)
    @PostMapping("/data/session/insert/events")
    fun dataInsertSessionEvents(@RequestBody body: String): ResponseEntity<String> {
        return withParsedBody<Events>(body){ events ->
            withErrorHandling("Error inserting session events") {
                val eventDTOs = events.events.stream()
                    .map { SessionEvent.fromCompressedJson(it) }
                    .map { SessionEventDTO.fromSessionEvent(it, events.sessionId) }
                    .toList()
                val (_,runTime) = runTimed { daoController.handleBulkInsertSessionEvents(eventDTOs) }
                log.debug("Inserted {} events in {}ms for session {}", eventDTOs.size, runTime, events.sessionId)
                Response.getOk().toResponseEntity()
            }
        }
    }

    @PostMapping("/data/session/select/events")
    fun dataSelectSessionEvents(@RequestBody body: String): ResponseEntity<String> {
        return withParsedBody<SessionEventDTO>(body) { sessionEventDTO ->
            withErrorHandling("Error selecting session events") {
                if(sessionEventDTO.eventId != null) {
                    Response.getOk(daoController.handleSelect(sessionEventDTO)).toResponseEntity()
                } else {
                    Response.getOk(daoController.handleSelectListSessionEvents(sessionEventDTO)).toResponseEntity()
                }
            }
        }
    }

    // =========================================================================== |
    // ============================== MISCELLANEOUS ============================== |
    // =========================================================================== |

    @GetMapping("/bcrypt")
    fun bcrypt(): ResponseEntity<String> {
        return readHtmlFile("static/bcrypt.html").toResponseEntity()
    }

    @PostMapping("/bcrypt/encrypt")
    fun bcryptSend(@RequestBody password : String): ResponseEntity<String> {
        return authService.textToBCrypt(password).toResponseEntity()
    }

    @GetMapping("/error")
    fun errorPage(): ResponseEntity<String> {
        val code = HttpStatus.BAD_REQUEST
        return readHtmlFile("static/error_page.html")
            .replace("{{message}}", "Something went wrong")
            .replace("{{timestamp}}", Date().toString())
            .replace("{{status}}", code.toString())
            .toResponseEntity(code)
    }

    private fun readHtmlFile(path: String): String {
        return resources.getResource("classpath:$path").inputStream
            .bufferedReader().use { it.readText() }
    }

    private inline fun <reified T> withParsedBody (
        body: String,
        onSuccess: (T) -> ResponseEntity<String>
    ): ResponseEntity<String> {
        val data: T
        try {
            data = fromJson(body)
        } catch (e: Exception) {
            log.error("Error parsing request", e)
            return Response.getError("Error parsing request").toResponseEntity(HttpStatus.BAD_REQUEST)
        }
        return onSuccess(data)
    }

    private inline fun withErrorHandling(
        logErrorMessage: String,
        block: () -> ResponseEntity<String>
    ): ResponseEntity<String> {
        return try {
            return block()
        } catch (e: Exception) {
            log.error(logErrorMessage, e)
            Response.getError(e).toResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(RestApiController::class.java)
    }
}