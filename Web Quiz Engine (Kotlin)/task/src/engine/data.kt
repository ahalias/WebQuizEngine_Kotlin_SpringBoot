package engine

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.persistence.*
import javax.validation.constraints.Pattern
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size



@Entity
@Table(name = "quizzes")
open class QuizBody {
    @Id
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
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "id")
    var quizzes: QuizBody? = null
}

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
}



class UserDetailsImpl(user: User) : UserDetails {
    private val username: String
    private val password: String
    private val rolesAndAuthorities: List<GrantedAuthority>

    init {
        username = user.email
        password = user.password
        rolesAndAuthorities = listOf<GrantedAuthority>(SimpleGrantedAuthority(user.role))
    }

    override fun getAuthorities() = rolesAndAuthorities

    override fun getPassword() = password

    override fun getUsername() = username

    // 4 remaining methods that just return true
    override fun isAccountNonExpired() = true

    override fun isAccountNonLocked() = true

    override fun isCredentialsNonExpired() = true

    override fun isEnabled() = true
}

@Repository
interface QuizRepository: JpaRepository<QuizBody, Long> {
    override fun findAll(): List<QuizBody>
    override fun findById(id: Long): Optional<QuizBody>
}



data class QuizResponse(val success: Boolean,
                        val feedback: String)

