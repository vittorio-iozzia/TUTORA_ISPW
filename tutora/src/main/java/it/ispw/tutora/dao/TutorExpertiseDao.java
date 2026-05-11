package it.ispw.tutora.dao;


import it.ispw.tutora.exception.DatabaseException;
import it.ispw.tutora.exception.DuplicateTutorExpertiseException;
import it.ispw.tutora.exception.TutorExpertiseNotFoundException;
import it.ispw.tutora.model.TutorExpertise;

import java.sql.Connection;
import java.util.List;

public interface TutorExpertiseDao {

    void insertExpertise(Connection conn, TutorExpertise expertise)
            throws DatabaseException, DuplicateTutorExpertiseException;


    void updateExpertise(Connection conn, TutorExpertise expertise)
            throws DatabaseException, TutorExpertiseNotFoundException;

    List<TutorExpertise> findByTutor(Connection conn, String tutorUsername)
            throws DatabaseException;

    TutorExpertise selectExpertise(Connection conn, String tutorUsername, String subcategoryName)
            throws DatabaseException, TutorExpertiseNotFoundException;

    void updateExpertiseStatus(Connection conn, TutorExpertise tutorExpertise )
            throws DatabaseException, TutorExpertiseNotFoundException;
}
