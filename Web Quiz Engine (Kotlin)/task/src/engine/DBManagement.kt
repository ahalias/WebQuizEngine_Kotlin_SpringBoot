package engine

import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class DBManagement(
    val completedQuizzesRepository: CompletedQuizzesRepository,
    val userRepo: UserRepository
) {

    fun addQuizToSaved(authentication: Authentication, quiz: QuizBody) {
        completedQuizzesRepository.save(CompletedQuizzes().apply {
            this.completedBy = authentication.name
            this.id = quiz.id
        })
    }

    fun saveUserToDb(user: User, encoder: PasswordEncoder) {
        user.password = encoder.encode(user.password)
        userRepo.save(user)
    }
}
