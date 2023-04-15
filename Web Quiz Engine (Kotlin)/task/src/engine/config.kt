package engine

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

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