package engine

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*
import javax.validation.constraints.Pattern
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size



@Entity
@Table(name = "quizzes")
open class QuizBody {
    @Id
    @Column(name = "quizzes_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0
    @NotBlank
    var title: String = ""
    @NotBlank
    var text: String = ""
    @Size(min=2)
    var options: Array<String> = emptyArray()
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    var answer: Array<Int> = emptyArray()
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "email", nullable = false)
    var author: User? = null
}

@Entity
@Table(name = "users")
open class User {
    @Id
    @Pattern(regexp = "[\\w.-]+@[\\w.-]+\\.[\\w]+")
    @NotBlank
    @Column(unique=true)
    var email: String = ""
    @NotBlank
    @Size(min=5)
    var password: String = ""
    var role: String = "USER"
}


@Entity
@Table(name = "completed_quizzes")
open class CompletedQuizzes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    var quiz_id: Long = 0
    @JsonIgnore
    var completedBy: String = ""
    var id: Long = 0
    var completedAt: LocalDateTime = LocalDateTime.now()
}


@Repository
interface CompletedQuizzesRepository : JpaRepository<CompletedQuizzes, Long> {
    fun findBycompletedByOrderByCompletedAtDesc(completedBy: String, pageable: Pageable): Page<CompletedQuizzes>?
    override fun findById(id: Long): Optional<CompletedQuizzes>
    fun findAllById(id: Long): List<CompletedQuizzes>

}

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
}


@Repository
interface QuizRepository: JpaRepository<QuizBody, Long> {
    override fun findAll(): List<QuizBody>
    override fun findById(id: Long): Optional<QuizBody>
}


data class QuizResponse(val success: Boolean,
                        val feedback: String)

