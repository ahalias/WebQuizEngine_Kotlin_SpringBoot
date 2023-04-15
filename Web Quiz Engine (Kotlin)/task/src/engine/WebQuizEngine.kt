package engine

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.*
import java.util.concurrent.ConcurrentHashMap
import javax.validation.Valid
import org.springframework.data.domain.PageRequest


@Service
class UserDetailsServiceImpl(
    val userRepository: UserRepository
) : UserDetailsService {

    @Throws(UsernameNotFoundException::class)
    override fun loadUserByUsername(username: String): UserDetails {
        val user: User = userRepository.findByEmail(username)
            ?: throw UsernameNotFoundException("Not found: $username")
        return UserDetailsImpl(user)
    }
}

@EnableWebSecurity
@Configuration(proxyBeanMethods = false)
class WebSecurityConfigurerImpl(val userRepository: UserRepository) : WebSecurityConfigurerAdapter() {
    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http.authorizeRequests()
            .antMatchers("/actuator/shutdown").permitAll()
            .mvcMatchers("/api/register").permitAll()
            .antMatchers("/api/**").authenticated()
            .and()
            .formLogin()
            .and()
            .csrf().disable() // disabling CSRF will allow sending POST request using Postman
            .httpBasic() // enables basic auth.
    }

    @Bean
    fun getEncoder() = BCryptPasswordEncoder()

    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.userDetailsService(UserDetailsServiceImpl(userRepository)).passwordEncoder(getEncoder())
    }
}



@RestController
class QuizQuestions(private val quizRepository: QuizRepository, val userRepo: UserRepository, val completedQuizzesRepository: CompletedQuizzesRepository, val encoder: PasswordEncoder
) {

    @PostMapping("/api/register")
    fun register(@RequestBody @Valid user: User): ResponseEntity<Map<String, Any>> {
        if (userRepo.findByEmail(user.email) != null) {
            val response = ConcurrentHashMap<String, Any>()
            response["status"] = "error"
            response["message"] = "Failed to register user"
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
        }
        try {
            user.password = encoder.encode(user.password)
            userRepo.save(user)
            val response = ConcurrentHashMap<String, Any>()
            response["status"] = "success"
            response["message"] = "User registered successfully ${user.email}, ${user.password}"
            return ResponseEntity.status(HttpStatus.OK).body(response)
        } catch(e: Exception) {
            val response = ConcurrentHashMap<String, Any>()
            response["status"] = "error"
            response["message"] = "Failed to register user: ${e.message}"
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
            }
    }

    @PostMapping("/api/quizzes")
    fun addQuiz(@RequestBody @Valid quizbody: QuizBody, authentication: Authentication): QuizBody {
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

    @GetMapping("/api/quizzes")
        fun getQuizzes(@RequestParam page: Int): ResponseEntity<Page<QuizBody>> {
        val quizPage = quizRepository.findAll(PageRequest.of(page, 10))
        return ResponseEntity.ok(quizPage)
    }


    @GetMapping("api/quizzes/{id}")
    fun resolveQuizById(@PathVariable id: Long): QuizBody {
        val quiz = quizRepository.findById(id).orElseThrow { QuizNotFound("Quiz not found") }
        return quiz
    }

    @DeleteMapping("api/quizzes/{id}")
    fun deleteQuiz(@PathVariable id: Long, authentication: Authentication): ResponseEntity<Map<String, Any>> {
        val quiz = quizRepository.findById(id).orElseThrow { QuizNotFound("Quiz not found") }
        val author = userRepo.findByEmail(authentication.name)
        if (author != quiz.author) throw Forbidden("You are not author") else {
        quizRepository.delete(quiz)
            val completedQuizList = completedQuizzesRepository.findAllById(id)
            completedQuizList.forEach { completedQuizzesRepository.delete(it) }
        val response = ConcurrentHashMap<String, Any>()
        response["status"] = "deleted"
        response["message"] = "Quiz successfully deleted"
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(response)

    }}

    @PostMapping("/api/quizzes/{id}/solve")
    fun solveQuiz(@PathVariable id: Long, authentication: Authentication, @RequestBody answer: Map<String, Array<Int>>): QuizResponse {
        val rightAnswer = QuizResponse( true, "Congratulations, you're right!")
        val wrongAnswer= QuizResponse(false,"Wrong answer! Please, try again.")
        val quiz = quizRepository.findById(id).orElseThrow { QuizNotFound("Quiz not found") }
            return if (quiz.answer.contentEquals(answer["answer"])) {

                completedQuizzesRepository.save(CompletedQuizzes().apply {
                    this.completedBy = authentication.name
                    this.id = quiz.id
                })
                rightAnswer

            }
            else if (answer["answer"].isNullOrEmpty() && quiz.answer.isEmpty()) {
                completedQuizzesRepository.save(CompletedQuizzes().apply {
                    this.completedBy = authentication.name
                    this.id = quiz.id
                })
                rightAnswer
            } else  QuizResponse(false,"Wrong answer! Please, try again.")
    }

    @GetMapping("/api/quizzes/completed")
    fun resolveCompletedQuizzes(@RequestParam page: Int, authentication: Authentication): ResponseEntity<Map<String, Any>> {
        val quizzes = completedQuizzesRepository.findBycompletedByOrderByCompletedAtDesc(authentication.name, PageRequest.of(page, 10))
        val response = ConcurrentHashMap<String, Any>()
        response["content"] = quizzes?.toList()?.sortedWith(compareBy { it.completedAt })?.reversed() ?: emptyList<Any>()
        return ResponseEntity.status(HttpStatus.OK).body(response)

    }

    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    class QuizNotFound(cause: String) : RuntimeException(cause)

    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    class WrongUserCredentials(cause: String) : RuntimeException(cause)

    @ResponseStatus(code = HttpStatus.FORBIDDEN)
    class Forbidden(cause: String) : RuntimeException(cause)

}


