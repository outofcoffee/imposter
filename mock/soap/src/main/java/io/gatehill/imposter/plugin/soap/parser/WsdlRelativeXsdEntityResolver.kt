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
class WsdlRelativeXsdEntityResolver : EntityResolver {

    companion object {
        val wsdlFolderPathThreadLocal: ThreadLocal<String> = ThreadLocal()
    }

    private val logger: Logger = LogManager.getLogger(this::class.java)

    override fun resolveEntity(publicId: String?, systemId: String?): InputSource? {
        logger.trace("Resolve {} relative to path {}", systemId, wsdlFolderPathThreadLocal.get())
        systemId ?: return null
        val wsdlFolderPath = wsdlFolderPathThreadLocal.get() ?: return null;

        // in certain occasions, XMLBeans prefixes the systemId (see StscState) -> strip it off
        val xsdFile = File(wsdlFolderPath, systemId.replace("project://local/", ""))
        if (Files.isRegularFile(xsdFile.toPath()) && xsdFile.name.lowercase().endsWith(".xsd")) {
            logger.debug("Resolved XSD {} relative to WSDL path", xsdFile)
            return InputSource(FileInputStream(xsdFile))
        }
        return null
    }

}