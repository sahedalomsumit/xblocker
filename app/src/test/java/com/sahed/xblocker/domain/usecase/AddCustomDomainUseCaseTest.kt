package com.sahed.xblocker.domain.usecase

import com.sahed.xblocker.data.repository.BlocklistRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AddCustomDomainUseCaseTest {

    private lateinit var repo: BlocklistRepository
    private lateinit var useCase: AddCustomDomainUseCase

    @Before
    fun setup() {
        repo = mockk()
        useCase = AddCustomDomainUseCase(repo)
    }

    @Test
    fun `blank input returns EmptyDomain`() = runTest {
        val result = useCase("")
        assertEquals(AddCustomDomainUseCase.Result.EmptyDomain, result)
    }

    @Test
    fun `invalid domain returns InvalidDomain`() = runTest {
        coEvery { repo.getAllActiveDomains() } returns emptyList()
        assertEquals(AddCustomDomainUseCase.Result.InvalidDomain, useCase("not-a-domain"))
        assertEquals(AddCustomDomainUseCase.Result.InvalidDomain, useCase("http://"))
        assertEquals(AddCustomDomainUseCase.Result.InvalidDomain, useCase("just text"))
    }

    @Test
    fun `valid domain is added`() = runTest {
        coEvery { repo.getAllActiveDomains() } returns emptyList()
        coEvery { repo.addDomain("example.com") } returns 1L
        val result = useCase("https://www.example.com/")
        assertEquals(AddCustomDomainUseCase.Result.Success(1L), result)
        coVerify { repo.addDomain("example.com") }
    }

    @Test
    fun `duplicate domain returns AlreadyExists`() = runTest {
        coEvery { repo.getAllActiveDomains() } returns listOf("example.com")
        val result = useCase("example.com")
        assertEquals(AddCustomDomainUseCase.Result.AlreadyExists, result)
    }

    @Test
    fun `wildcard domain valid`() = runTest {
        coEvery { repo.getAllActiveDomains() } returns emptyList()
        coEvery { repo.addDomain("*.example.com") } returns 2L
        val result = useCase("*.example.com")
        assertEquals(AddCustomDomainUseCase.Result.Success(2L), result)
    }

    @Test
    fun `strips http prefix and www`() = runTest {
        coEvery { repo.getAllActiveDomains() } returns emptyList()
        coEvery { repo.addDomain("test.org") } returns 3L
        useCase("http://www.test.org/path")
        coVerify { repo.addDomain("test.org") }
    }
}
