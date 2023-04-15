package engine

import org.springframework.security.core.Authentication

fun addQuizToSaved(authentication: Authentication, quiz: QuizBody, completedQuizzesRepository: CompletedQuizzesRepository): QuizResponse {
    completedQuizzesRepository.save(CompletedQuizzes().apply {
        this.completedBy = authentication.name
        this.id = quiz.id
    })
    return QuizResponse( true, "Congratulations, you're right!")
}