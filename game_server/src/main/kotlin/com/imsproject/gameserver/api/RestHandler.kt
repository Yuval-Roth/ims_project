package com.imsproject.gameserver.api

import com.imsproject.common.gameserver.GameRequest
import com.imsproject.common.gameserver.SessionEvent
import com.imsproject.common.utils.Response
import com.imsproject.gameserver.dataAccess.DAOController
import com.imsproject.gameserver.toResponseEntity
import com.imsproject.common.utils.fromJson
import com.imsproject.gameserver.business.GameRequestFacade
import com.imsproject.gameserver.business.ParticipantController
import com.imsproject.gameserver.business.auth.AuthController
import com.imsproject.gameserver.business.auth.Credentials
import com.imsproject.gameserver.dataAccess.implementations.ParticipantsDAO
import com.imsproject.gameserver.dataAccess.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
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
class RestHandler(
    private val gameRequestFacade: GameRequestFacade,
    private val authController: AuthController,
    private val participantController: ParticipantController,
    private val daoController: DAOController,
    private val resources: ResourceLoader,
    private val participantsDAO: ParticipantsDAO
) : ErrorController {

    private val dispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
    private val scope = CoroutineScope(dispatcher)

    @RequestMapping("/login", method = [RequestMethod.POST, RequestMethod.GET])
    fun login(@RequestHeader(value = "Authorization") header : String?): ResponseEntity<String> {
        if(header == null){
            return Response.getError("No Authorization header").toResponseEntity(HttpStatus.BAD_REQUEST)
        }
        // base64 encoded user:password
        val credentials = header.split(" ")[1]
        val decoded = String(Base64.getDecoder().decode(credentials))
        val split = decoded.split(":")
        val userId = split[0]
        val password = split[1]
        return authController.authenticateUser(userId, password).toResponseEntity()
    }

    @PostMapping("/manager")
    fun manager(@RequestBody body : String): ResponseEntity<String> {
        val request: GameRequest
        try{
            request = fromJson(body)
        } catch(e: Exception){
            log.error("Error parsing request", e)
            return Response.getError("Error parsing request").toResponseEntity(HttpStatus.BAD_REQUEST)
        }

        try {
            return gameRequestFacade.handleGameRequest(request).toResponseEntity()
        } catch (e: Exception) {
            log.error("Error handling game request", e)
            return Response.getError(e).toResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    // =========================================================================== |
    // ============================= ADMIN ENDPOINTS ============================= |
    // =========================================================================== |

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping("/operators/{action}", method = [RequestMethod.POST, RequestMethod.GET])
    fun operators(
        @PathVariable action: String,
        @RequestBody body : String?
    ): ResponseEntity<String> {
        val credentials : Credentials = if(body != null){
            try{
                fromJson(body)
            } catch(e: Exception){
                return Response.getError("Error parsing request").toResponseEntity(HttpStatus.BAD_REQUEST)
            }
        } else {
            Credentials("","",null)
        }
        try{
            when(action){
                "add" -> authController.createUser(credentials)
                "remove" -> authController.deleteUser(credentials.userId)
                "get" -> {
                    val users = authController.getAllUsers()
                    return Response.getOk(users).toResponseEntity()
                }
                else -> Response.getError("Invalid action").toResponseEntity(HttpStatus.BAD_REQUEST)
            }
        } catch(e: IllegalArgumentException){
            return Response.getError(e).toResponseEntity(HttpStatus.BAD_REQUEST)
        } catch(e: Exception){
            return Response.getError(e).toResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
        return Response.getOk().toResponseEntity()
    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping("/participants/{action}", method = [RequestMethod.POST, RequestMethod.GET])
    fun participants(
        @PathVariable action: String,
        @RequestBody body : String?
    ): ResponseEntity<String> {
        val participant : ParticipantDTO = if(body != null){
            try{
                fromJson(body)
            } catch(e: Exception){
                return Response.getError("Error parsing request").toResponseEntity(HttpStatus.BAD_REQUEST)
            }
        } else {
            ParticipantDTO(null,null,null,null,null,null,null)
        }
        try{
            when(action){
                "add" -> {
                    log.debug("Adding participant: {}", participant)
                    val id = participantController.addParticipant(participant)
                    log.debug("Successfully Added participant with id: {}", id)
                    return Response.getOk(id).toResponseEntity()
                }
                "remove" ->{
                    log.debug("Removing participant: {}", participant)
                    val pid = participant.pid ?: throw IllegalArgumentException("Participant id not provided")
                    participantController.remove(pid)
                    log.debug("Successfully removed participant with id: {}", pid)
                }
                "get" -> {
                    val participants = participantController.getAll()
                    return Response.getOk(participants).toResponseEntity()
                }
                else -> Response.getError("Invalid action").toResponseEntity(HttpStatus.BAD_REQUEST)
            }
        } catch (e: IllegalArgumentException) {
            log.debug("Error handling participant request", e)
            return Response.getError(e).toResponseEntity(HttpStatus.BAD_REQUEST)
        } catch (e: Exception) {
            log.error("Error handling participant request", e)
            return Response.getError(e).toResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
        }
        return Response.getOk().toResponseEntity()
    }

    // =========================================================================== |
    // ============================ DATA ENDPOINTS =============================== |
    // =========================================================================== |

    @PostMapping("/data/participant/select")
    fun dataSelectParticipants(@RequestBody body: String): ResponseEntity<String> {
        val participantDTO: ParticipantDTO

        try {
            participantDTO = fromJson<ParticipantDTO>(body)
            return if(participantDTO.pid == null) {
                Response.getOk(daoController.handleSelectAllParticipants()).toResponseEntity()
            } else {
                Response.getOk(daoController.handleSelect(participantDTO)).toResponseEntity()
            }
        } catch(e: Exception) {
            return Response.getError(e).toResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

    // ===== Experiment ===== |

    @PostMapping("/data/experiment/select/names")
    fun dataSelectExperiments(@RequestBody body: String): ResponseEntity<String> {
        val experimentDTO: ExperimentDTO

        try {
            experimentDTO = fromJson<ExperimentDTO>(body)
            return Response.getOk(daoController.handleSelectAllExperimentsWithNames()).toResponseEntity()
        } catch(e: Exception) {
            return Response.getError(e).toResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

    private data class QnA(val question: String, val answer: String)
    private data class ExperimentFeedback(val expId: Int, val pid: Int, val qnas: List<QnA>)
    @PostMapping("/data/experiment/insert/feedback")
    fun dataInsertExperimentFeedback(@RequestBody body: String): ResponseEntity<String> {
        val feedbackDTOs: List<ExperimentFeedbackDTO>
        val feedback: ExperimentFeedback

        try {
            feedback = fromJson<ExperimentFeedback>(body)
            feedbackDTOs = feedback.qnas.map {
                ExperimentFeedbackDTO(
                    expId = feedback.expId,
                    pid = feedback.pid,
                    question = it.question,
                    answer = it.answer
                )
            }
        } catch (e: Exception) {
            return Response.getError(e).toResponseEntity(HttpStatus.BAD_REQUEST)
        }

        scope.launch {
            val startTime = System.nanoTime()
            daoController.handleBulkInsertExperimentFeedback(feedbackDTOs)
            val endTime = System.nanoTime()
            log.debug("Inserted {} feedback entries in {}ms for expId {}", feedbackDTOs.size, (endTime - startTime) / 1_000_000, feedback.expId)
        }

        return Response.getOk().toResponseEntity()
    }

    @PostMapping("/data/experiment/select/feedback")
    fun dataSelectExperimentsFeedback(@RequestBody body: String): ResponseEntity<String> {
        val experimentFeedbackDTO: ExperimentFeedbackDTO

        try {
            experimentFeedbackDTO = fromJson<ExperimentFeedbackDTO>(body)
            return Response.getOk(daoController.handleSelectListExperimentsFeedback(experimentFeedbackDTO)).toResponseEntity()
        } catch(e: Exception) {
            return Response.getError(e).toResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

    // ===== Session ===== |

    @PostMapping("/data/session/select")
    fun dataSelectSessions(@RequestBody body: String): ResponseEntity<String> {
        val sessionDTO: SessionDTO

        try {
            sessionDTO = fromJson<SessionDTO>(body)
            return if(sessionDTO.sessionId != null) {
                Response.getOk(daoController.handleSelect(sessionDTO)).toResponseEntity()
            } else {
                Response.getOk(daoController.handleSelectListSessions(sessionDTO)).toResponseEntity()
            }
        } catch(e: Exception) {
            return Response.getError(e).toResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

    private data class sessionFeedback(val expId: Int, val sessionId: Int, val pid: Int, val qnas: List<QnA>)
    @PostMapping("/data/session/insert/feedback")
    fun dataInsertSessionFeedback(@RequestBody body: String): ResponseEntity<String> {
        val feedbackDTOs: List<SessionFeedbackDTO>
        val feedback: sessionFeedback

        try {
            feedback = fromJson<sessionFeedback>(body)
            feedbackDTOs = feedback.qnas.map {
                SessionFeedbackDTO(
                    expId = feedback.expId,
                    sessionId = feedback.sessionId,
                    pid = feedback.pid,
                    question = it.question,
                    answer = it.answer
                )
            }
        } catch (e: Exception) {
            return Response.getError(e).toResponseEntity(HttpStatus.BAD_REQUEST)
        }

        scope.launch {
            val startTime = System.nanoTime()
            daoController.handleBulkInsertSessionFeedback(feedbackDTOs)
            val endTime = System.nanoTime()
            log.debug("Inserted {} feedback entries in {}ms for expId {}", feedbackDTOs.size, (endTime - startTime) / 1_000_000, feedback.expId)
        }

        return Response.getOk().toResponseEntity()
    }

    @PostMapping("/data/session/select/feedback")
    fun dataSelectSessionsFeedback(@RequestBody body: String): ResponseEntity<String> {
        val sessionFeedbackDTO: SessionFeedbackDTO

        try {
            sessionFeedbackDTO = fromJson<SessionFeedbackDTO>(body)
            return Response.getOk(daoController.handleSelectListSessionsFeedback(sessionFeedbackDTO)).toResponseEntity()
        } catch(e: Exception) {
            return Response.getError(e).toResponseEntity(HttpStatus.BAD_REQUEST)
        }
    }

    private data class Events(val sessionId: Int, val events: List<String>)
    @PostMapping("/data/session/insert/events")
    fun data(@RequestBody body: String): ResponseEntity<String> {

        val eventDTOs: List<SessionEventDTO>?
        val events: Events
        try {
            events = fromJson<Events>(body)
            eventDTOs = events.events.stream()
                .map { SessionEvent.fromCompressedJson(it) }
                .map { SessionEventDTO.fromSessionEvent(it, events.sessionId) }
                .toList()
        } catch(e: Exception) {
            return Response.getError(e).toResponseEntity(HttpStatus.BAD_REQUEST)
        }
        scope.launch {
            val startTime = System.nanoTime()
            daoController.handleBulkInsertSessionEvents(eventDTOs)
            val endTime = System.nanoTime()
            log.debug("Inserted {} events in {}ms for session {}", eventDTOs.size, (endTime - startTime) / 1_000_000, events.sessionId)
        }
        return Response.getOk().toResponseEntity()
    }

    @PostMapping("/data/session/select/events")
    fun dataSelectSessionEvents(@RequestBody body: String): ResponseEntity<String> {
        val sessionEventDTO: SessionEventDTO

        try {
            sessionEventDTO = fromJson<SessionEventDTO>(body)
            return if(sessionEventDTO.eventId != null) {
                Response.getOk(daoController.handleSelect(sessionEventDTO)).toResponseEntity()
            } else {
                Response.getOk(daoController.handleSelectListSessionEvents(sessionEventDTO)).toResponseEntity()
            }
        } catch(e: Exception) {
            return Response.getError(e).toResponseEntity(HttpStatus.BAD_REQUEST)
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
        return authController.textToBCrypt(password).toResponseEntity()
    }

    @GetMapping("/error")
    fun errorPage(): ResponseEntity<String> {
        val code = HttpStatus.BAD_REQUEST
        return readHtmlFile("static/error_page.html")
            .replace("[MESSAGE]", "Something went wrong")
            .replace("[TIME_STAMP]", Date().toString())
            .replace("[STATUS]", code.toString())
            .toResponseEntity(code)
    }

    private fun readHtmlFile(path: String): String {
        return resources.getResource("classpath:$path").inputStream
            .bufferedReader().use { it.readText() }
    }

    companion object {
        private val log = LoggerFactory.getLogger(RestHandler::class.java)
    }
}