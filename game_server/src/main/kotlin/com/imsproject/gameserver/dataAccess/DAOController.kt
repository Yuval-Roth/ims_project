package com.imsproject.gameserver.dataAccess

import com.imsproject.common.dataAccess.DaoException
import com.imsproject.gameserver.dataAccess.implementations.*

import com.imsproject.gameserver.dataAccess.models.*
import org.slf4j.LoggerFactory

import org.springframework.stereotype.Component


@Suppress("UNCHECKED_CAST")
@Component
class DAOController(
    private val participantDAO: ParticipantsDAO,
    private val experimentDAO: ExperimentsDAO,
    private val sessionDAO: SessionsDAO,
    private val sessionEventDAO: SessionEventsDAO,
    private val experimentsFeedbackDAO: ExperimentsFeedbackDAO,
    private val sessionsFeedbackDAO: SessionsFeedbackDAO
) {

    @Throws(DaoException::class)
    fun handleInsert(dto: ParticipantDTO): Int {
        return participantDAO.insert(dto)
    }

    @Throws(DaoException::class)
    fun handleInsert(dto: ExperimentDTO): Int {
        return experimentDAO.insert(dto)
    }

    @Throws(DaoException::class)
    fun handleInsert(dto: SessionDTO): Int {
        return sessionDAO.insert(dto)
    }

    @Throws(DaoException::class)
    fun handleInsert(dto: SessionEventDTO): Int {
        return sessionEventDAO.insert(dto)
    }

    @Throws(DaoException::class)
    fun handleInsertAllParticipants(dtos: List<ParticipantDTO>): List<Int> =
        participantDAO.insertAll(dtos)

    @Throws(DaoException::class)
    fun handleInsertAllExperiments(dtos: List<ExperimentDTO>): List<Int> =
        experimentDAO.insertAll(dtos)

    @Throws(DaoException::class)
    fun handleInsertAllSessions(dtos: List<SessionDTO>): List<Int> =
        sessionDAO.insertAll(dtos)

    @Throws(DaoException::class)
    fun handleInsertAllSessionEvents(dtos: List<SessionEventDTO>): List<Int> =
        sessionEventDAO.insertAll(dtos)

    @Throws(DaoException::class)
    fun handleBulkInsertParticipants(dtos: List<ParticipantDTO>): Int =
        throw UnsupportedOperationException("Bulk insert not supported for participants")

    @Throws(DaoException::class)
    fun handleBulkInsertExperiments(dtos: List<ExperimentDTO>): Int =
        throw UnsupportedOperationException("Bulk insert not supported for experiments")

    @Throws(DaoException::class)
    fun handleBulkInsertSessions(dtos: List<SessionDTO>): Int =
        throw UnsupportedOperationException("Bulk insert not supported for sessions")

    @Throws(DaoException::class)
    fun handleBulkInsertSessionEvents(dtos: List<SessionEventDTO>): Int =
        sessionEventDAO.bulkInsert(dtos)

    @Throws(DaoException::class)
    fun handleBulkInsertExperimentFeedback(dtos: List<ExperimentFeedbackDTO>) {
        experimentsFeedbackDAO.bulkInsert(dtos)
    }

    fun handleBulkInsertSessionFeedback(dtos: List<SessionFeedbackDTO>) {
        sessionsFeedbackDAO.bulkInsert(dtos)
    }

    @Throws(DaoException::class)
    fun handleExists(pk: ParticipantPK): Boolean {
        return participantDAO.exists(pk)
    }

    @Throws(DaoException::class)
    fun handleExists(pk: ExperimentPK): Boolean {
        return experimentDAO.exists(pk)
    }

    @Throws(DaoException::class)
    fun handleExists(pk: SessionPK): Boolean {
        return sessionDAO.exists(pk)
    }

    @Throws(DaoException::class)
    fun handleExists(pk: SessionEventPK): Boolean {
        return sessionEventDAO.exists(pk)
    }



    @Throws(DaoException::class)
    fun handleDelete(participantDTO: ParticipantDTO) {
        if (participantDTO.pid == null) throw Exception("A participant id was not provided for deletion")
        participantDAO.delete(ParticipantPK(participantDTO.pid))
    }

    @Throws(DaoException::class)
    fun handleDelete(exp: ExperimentDTO) {
        if (exp.expId == null) throw Exception("An experiment id was not provided for deletion")
        experimentDAO.delete(ExperimentPK(exp.expId))
    }

    @Throws(DaoException::class)
    fun handleDelete(sessionDTO: SessionDTO) {
        if (sessionDTO.sessionId == null) throw Exception("A session id was not provided for deletion")
        sessionDAO.delete(SessionPK(sessionDTO.sessionId))
    }

    @Throws(DaoException::class)
    fun handleDelete(sessionEventDTO: SessionEventDTO) {
        if (sessionEventDTO.eventId == null) throw Exception("A session event id was not provided for deletion")
        sessionEventDAO.delete(SessionEventPK(sessionEventDTO.eventId))
    }

    @Throws(DaoException::class)
    fun handleUpdate(participantDTO: ParticipantDTO) {
        if (participantDTO.pid == null) throw Exception("A participant id was not provided for update")
        participantDAO.update(participantDTO)
    }

    @Throws(DaoException::class)
    fun handleUpdate(exp: ExperimentDTO) {
        if (exp.expId == null) throw Exception("An experiment id was not provided for update")
        experimentDAO.update(exp)
    }

    @Throws(DaoException::class)
    fun handleUpdate(sessionDTO: SessionDTO) {
        if (sessionDTO.sessionId == null) throw Exception("A session id was not provided for update")
        sessionDAO.update(sessionDTO)
    }

    @Throws(DaoException::class)
    fun handleUpdate(sessionEventDTO: SessionEventDTO) {
        if (sessionEventDTO.eventId == null) throw Exception("A session event id was not provided for update")
        sessionEventDAO.update(sessionEventDTO)
    }

    @Throws(DaoException::class)
    fun handleSelect(participantDTO: ParticipantDTO): Any {
        if (participantDTO.pid == null) throw Exception("A participant id was not provided for selection")
        return participantDAO.select(ParticipantPK(participantDTO.pid))
    }

    @Throws(DaoException::class)
    fun handleSelect(exp: ExperimentDTO): ExperimentDTO {
        if (exp.expId == null) throw Exception("An experiment id was not provided for selection")
        return experimentDAO.select(ExperimentPK(exp.expId))
    }

    @Throws(DaoException::class)
    fun handleSelect(sessionDTO: SessionDTO): Any {
        if (sessionDTO.sessionId == null) throw Exception("A session id was not provided for selection")

        return sessionDAO.select(SessionPK(sessionDTO.sessionId))
    }

    @Throws(DaoException::class)
    fun handleSelect(sessionEventDTO: SessionEventDTO): Any {
        if (sessionEventDTO.eventId == null) throw Exception("A session event id was not provided for selection")
        return sessionEventDAO.select(SessionEventPK(sessionEventDTO.eventId))
    }

    fun handleSelectAllParticipants(): List<ParticipantDTO> = participantDAO.selectAll()
    fun handleSelectAllExperiments(): List<ExperimentDTO> = experimentDAO.selectAll()
    fun handleSelectAllExperimentsWithNames(): List<ExperimentWithParticipantNamesDTO> {
        return experimentDAO.selectWithParticipantNames()
    }
    fun handleSelectAllSessions(): List<SessionDTO> = sessionDAO.selectAll()
    fun handleSelectAllSessionEvents(): List<SessionEventDTO> = sessionEventDAO.selectAll()
    fun handleSelectAllExperimentsFeedback(): List<ExperimentFeedbackDTO> = experimentsFeedbackDAO.selectAll()
    fun handleSelectAllSessionsFeedback(): List<SessionFeedbackDTO> = sessionsFeedbackDAO.selectAll()


    fun handleSelectListSessions(sessionDTO: SessionDTO): List<SessionDTO> {
        val data = sessionDTO.toNonNullMap()
        if (data.isEmpty())
            return handleSelectAllSessions()
        return sessionDAO.selectAggregate(data.keys.toTypedArray(), data.values.toTypedArray())
    }

    fun handleSelectListSessionEvents(sessionEventDTO: SessionEventDTO): List<SessionEventDTO> {
        val data = sessionEventDTO.toNonNullMap()
        if (data.isEmpty())
            return handleSelectAllSessionEvents()
        return sessionEventDAO.selectAggregate(data.keys.toTypedArray(), data.values.toTypedArray())
    }

    fun handleSelectListExperimentsFeedback(experimentsFeedbackDTO: ExperimentFeedbackDTO): List<ExperimentFeedbackDTO> {
        val data = experimentsFeedbackDTO.toNonNullMap()
        if (data.isEmpty())
            return handleSelectAllExperimentsFeedback()
        return experimentsFeedbackDAO.selectAggregate(data.keys.toTypedArray(), data.values.toTypedArray())
    }

    fun handleSelectListSessionsFeedback(sessionFeedbackDTO: SessionFeedbackDTO): List<Any> {
        val data = sessionFeedbackDTO.toNonNullMap()
        if (data.isEmpty())
            return handleSelectAllExperimentsFeedback()
        return sessionsFeedbackDAO.selectAggregate(data.keys.toTypedArray(), data.values.toTypedArray())
    }

    companion object {
        private val log = LoggerFactory.getLogger(DAOController::class.java)
    }
}
