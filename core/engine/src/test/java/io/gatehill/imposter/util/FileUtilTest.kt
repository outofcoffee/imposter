package io.gatehill.imposter.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createTempDirectory

/**
 * Tests for [FileUtil].
 */
class FileUtilTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun validatePathShouldAllowValidPaths() {
        val configDir = tempDir.toFile()
        val validPaths = listOf(
            "file.txt",
            "dir/file.txt",
            "dir/subdir/file.txt",
            "./file.txt",
            "dir/./file.txt"
        )

        validPaths.forEach { path ->
            val result = FileUtil.validatePath(path, configDir)
            assertTrue(result.startsWith(configDir.canonicalFile.toPath()), "Path should start with config directory")
            assertEquals(
                configDir.canonicalFile.toPath().resolve(path).normalize(),
                result,
                "Path should resolve correctly",
            )
        }
    }

    @Test
    fun validatePathShouldPreventDirectoryTraversalAttempts() {
        val configDir = tempDir.toFile()
        val maliciousPaths = listOf(
            "../file.txt",
            "../../file.txt",
            "../etc/passwd",
            "dir/../../../etc/passwd",
            "dir/subdir/../../../../etc/passwd",
            "dir/./../../file.txt",
        )

        maliciousPaths.forEach { path ->
            assertThrows(SecurityException::class.java) {
                FileUtil.validatePath(path, configDir)
            }
        }
    }

    @Test
    fun validatePathShouldHandleAbsolutePathsCorrectly() {
        val configDir = tempDir.toFile()
        val absolutePath = File(configDir, "file.txt").absolutePath

        val result = FileUtil.validatePath(absolutePath, configDir)
        assertTrue(result.startsWith(configDir.canonicalFile.toPath()), "Path should start with config directory")
    }

    @Test
    fun validatePathShouldHandleSymbolicLinksWithinAllowedDirectory() {
        val configDir = tempDir.toFile()
        val subDir = File(configDir, "subdir").apply { mkdir() }
        val targetFile = File(subDir, "target.txt").apply { writeText("test") }
        val symlink = File(configDir, "link.txt")

        // Create symbolic link if running with sufficient permissions
        try {
            java.nio.file.Files.createSymbolicLink(symlink.toPath(), targetFile.toPath())
            
            val result = FileUtil.validatePath("link.txt", configDir)
            assertTrue(result.startsWith(configDir.canonicalFile.toPath()), "Path should start with config directory")
            
        } catch (e: Exception) {
            // Skip test if unable to create symlinks (e.g. insufficient permissions)
            println("Skipping symlink test due to: ${e.message}")
        }
    }

    @Test
    fun validatePathShouldPreventSymlinkTraversalAttacks() {
        val configDir = tempDir.toFile()
        val outsideDir = createTempDirectory().toFile()
        val targetFile = File(outsideDir, "target.txt").apply { writeText("test") }
        val symlink = File(configDir, "malicious-link.txt")

        try {
            java.nio.file.Files.createSymbolicLink(symlink.toPath(), targetFile.toPath())
            
            assertThrows(SecurityException::class.java) {
                FileUtil.validatePath("malicious-link.txt", configDir)
            }
            
        } catch (e: Exception) {
            // Skip test if unable to create symlinks
            println("Skipping symlink traversal test due to: ${e.message}")
        } finally {
            outsideDir.deleteRecursively()
        }
    }
} 