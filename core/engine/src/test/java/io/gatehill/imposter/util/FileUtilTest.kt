package io.gatehill.imposter.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * Tests for [FileUtil].
 */
class FileUtilTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun validatePathShouldAllowValidPaths() {
        val configDir = tempFolder.root
        val validPaths = listOf(
            "file.txt",
            "dir/file.txt",
            "dir/subdir/file.txt",
            "./file.txt",
            "dir/./file.txt"
        )

        validPaths.forEach { path ->
            val result = FileUtil.validatePath(path, configDir)
            assertTrue("Path should start with config directory", result.startsWith(configDir.canonicalFile.toPath()))
            assertEquals(
                "Path should resolve correctly",
                configDir.canonicalFile.toPath().resolve(path).normalize(),
                result
            )
        }
    }

    @Test
    fun validatePathShouldPreventDirectoryTraversalAttempts() {
        val configDir = tempFolder.root
        val maliciousPaths = listOf(
            "../file.txt",
            "../../file.txt",
            "../etc/passwd",
            "dir/../../../etc/passwd",
            "dir/subdir/../../../../etc/passwd",
            "dir/./../../file.txt",
        )

        maliciousPaths.forEach { path ->
            try {
                FileUtil.validatePath(path, configDir)
                fail("Should throw SecurityException for path: $path")
            } catch (e: SecurityException) {
                // Expected
            }
        }
    }

    @Test
    fun validatePathShouldHandleAbsolutePathsCorrectly() {
        val configDir = tempFolder.root
        val absolutePath = File(configDir, "file.txt").absolutePath

        val result = FileUtil.validatePath(absolutePath, configDir)
        assertTrue("Path should start with config directory", result.startsWith(configDir.canonicalFile.toPath()))
    }

    @Test
    fun validatePathShouldHandleSymbolicLinksWithinAllowedDirectory() {
        val configDir = tempFolder.root
        val subDir = File(configDir, "subdir").apply { mkdir() }
        val targetFile = File(subDir, "target.txt").apply { writeText("test") }
        val symlink = File(configDir, "link.txt")

        // Create symbolic link if running with sufficient permissions
        try {
            java.nio.file.Files.createSymbolicLink(symlink.toPath(), targetFile.toPath())
            
            val result = FileUtil.validatePath("link.txt", configDir)
            assertTrue("Path should start with config directory", result.startsWith(configDir.canonicalFile.toPath()))
            
        } catch (e: Exception) {
            // Skip test if unable to create symlinks (e.g. insufficient permissions)
            println("Skipping symlink test due to: ${e.message}")
        }
    }

    @Test
    fun validatePathShouldPreventSymlinkTraversalAttacks() {
        val configDir = tempFolder.root
        val outsideDir = createTempDirectory().toFile()
        val targetFile = File(outsideDir, "target.txt").apply { writeText("test") }
        val symlink = File(configDir, "malicious-link.txt")

        try {
            java.nio.file.Files.createSymbolicLink(symlink.toPath(), targetFile.toPath())
            
            try {
                FileUtil.validatePath("malicious-link.txt", configDir)
                fail("Should throw SecurityException for malicious symlink")
            } catch (e: SecurityException) {
                // Expected
            }
            
        } catch (e: Exception) {
            // Skip test if unable to create symlinks
            println("Skipping symlink traversal test due to: ${e.message}")
        } finally {
            outsideDir.deleteRecursively()
        }
    }
} 