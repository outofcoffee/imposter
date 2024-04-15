package io.gatehill.imposter.plugin.soap.parser

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files

/**
 * EntityResolver to resolve XSDs included relative to the WSDL file.
 */
class WsdlRelativeXsdEntityResolver(
    private val wsdlDir: File
) : EntityResolver {
    private val logger: Logger = LogManager.getLogger(this::class.java)

    override fun resolveEntity(publicId: String?, systemId: String?): InputSource? {
        logger.trace("Resolve {} relative to path {}", systemId, wsdlDir)
        systemId ?: return null

        // in certain occasions, XMLBeans prefixes the systemId (see StscState) -> strip it off
        val xsdFile = File(wsdlDir.absoluteFile, systemId.replace("project://local/", ""))
        if (Files.isRegularFile(xsdFile.toPath()) && xsdFile.name.lowercase().endsWith(".xsd")) {
            logger.debug("Resolved XSD {} relative to WSDL path", xsdFile)

            // according to the InputSource docs, the parser using this resolver
            // should manage the clean-up of the stream.
            return InputSource(FileInputStream(xsdFile))
        }
        return null
    }
}
