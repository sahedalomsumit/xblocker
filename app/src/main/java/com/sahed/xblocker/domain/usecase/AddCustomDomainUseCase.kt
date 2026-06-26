package com.sahed.xblocker.domain.usecase

import com.sahed.xblocker.data.repository.BlocklistRepository
import javax.inject.Inject

class AddCustomDomainUseCase @Inject constructor(
    private val repo: BlocklistRepository
) {
    sealed class Result {
        data class Success(val id: Long) : Result()
        data object EmptyDomain : Result()
        data object InvalidDomain : Result()
        data object AlreadyExists : Result()
    }

    suspend operator fun invoke(rawInput: String): Result {
        val domain = rawInput
            .trim()
            .lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .trimEnd('/')

        if (domain.isBlank()) return Result.EmptyDomain

        // Basic domain validation (allows wildcards like *.example.com)
        val domainRegex = Regex("""^(\*\.)?([a-z0-9]([a-z0-9\-]{0,61}[a-z0-9])?\.)+[a-z]{2,}$""")
        if (!domainRegex.matches(domain)) return Result.InvalidDomain

        val existing = repo.getAllActiveDomains()
        if (existing.any { it.equals(domain, ignoreCase = true) }) return Result.AlreadyExists

        val id = repo.addDomain(domain)
        return if (id > 0) Result.Success(id) else Result.InvalidDomain
    }
}
