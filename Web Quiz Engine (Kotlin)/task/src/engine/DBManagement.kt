package engine

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service


@Service
class DBManagement(
    private val completedQuizzesRepository: CompletedQuizzesRepository,
    private val userRepo: UserRepository,
    private val quizRepository: QuizRepository
) {

    fun addQuizToSaved(authentication: Authentication, quiz: QuizBody) {
        completedQuizzesRepository.save(CompletedQuizzes().apply {
            this.completedBy = authentication.name
            this.id = quiz.id
        })
    }

    fun registerUser(user: User, encoder: PasswordEncoder) {
        user.password = encoder.encode(user.password)
        userRepo.save(user)
    }

    fun saveUserToDb(authentication: Authentication, quizbody: QuizBody): QuizBody {
        val author = userRepo.findByEmail(authentication.name)
        val quiz = QuizBody().apply {
            this.title = quizbody.title
            this.text = quizbody.text
            this.options = quizbody.options
            this.answer = quizbody.answer
            this.author = author
        }
        quizRepository.save(quiz)
        return quiz
    }

    fun findAllQuizzes(page: Int): Page<QuizBody> = quizRepository.findAll(PageRequest.of(page, 10))

    fun findQuizById(id: Long): QuizBody = quizRepository.findById(id).orElseThrow { QuizNotFound("Quiz not found") }

    fun deleteQuiz(id: Long, authentication: Authentication) {
        val quiz = quizRepository.findById(id).orElseThrow { QuizNotFound("Quiz not found") }
        val author = userRepo.findByEmail(authentication.name)
        if (author != quiz.author) throw Forbidden("You are not author") else {
            quizRepository.delete(quiz)
        }
    }
    fun deleteCompletedQuiz(id: Long) = completedQuizzesRepository.findAllById(id).forEach { completedQuizzesRepository.delete(it) }

    fun findCompletedQuizzesByAuthor(page: Int, authentication: Authentication): Page<CompletedQuizzes>? = completedQuizzesRepository.findBycompletedByOrderByCompletedAtDesc(authentication.name, PageRequest.of(page, 10))
}
