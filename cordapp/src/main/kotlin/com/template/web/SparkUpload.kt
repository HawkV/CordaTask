package com.template.web

import net.corda.client.rpc.CordaRPCClient
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.NetworkHostAndPort
import spark.Request
import spark.Spark.*
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.servlet.MultipartConfigElement
import javax.servlet.ServletException
import javax.servlet.http.Part
import com.template.flows.UploadFlows.UploadFlow
import freemarker.cache.ClassTemplateLoader
import freemarker.template.Configuration
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.getOrThrow
import spark.ModelAndView
import spark.Service
import spark.template.freemarker.FreeMarkerEngine
import java.util.HashMap

object SparkUpload{

    @JvmStatic
    fun main(args: Array<String>) {
        val freeMarkerEngine = initFreemarker(this::class.java)


        val proxy = setConnection(args.getOrNull(1) ?: "localhost:10005")
        val uploadDir = File("upload")
        uploadDir.mkdir() // create the upload directory if it doesn't exist
        staticFiles.externalLocation("upload")

        val http = Service.ignite().port(args.getOrNull(0)?.toInt()?:1234)
        http.staticFileLocation("/spark")

        http.get("/"
        ) { _, _ ->

                    val model = HashMap<String, Any>()
                    model["status"] = ""
                    freeMarkerEngine.render(ModelAndView(model, "SparkUpload.ftl"))
        }

        http.post("/") { req, _ ->

            req.attribute("org.eclipse.jetty.multipartConfig", MultipartConfigElement("/temp"))

            val tempFile = Files.createTempFile(uploadDir.toPath(), "", getFileName(req.raw().getPart("uploaded_file")))


            req.raw().getPart("uploaded_file").inputStream.use { // getPart needs to use same "name" as input field in form
                input ->
                Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING)

            }

            val fo = Files.createTempFile(uploadDir.toPath(), "s", ".zip")

            ZipOutputStream(fo.toFile().outputStream()).use { zos ->
                zos.putNextEntry(ZipEntry(tempFile.fileName.toString()))
                Files.newInputStream(tempFile).use {
                    it.copyTo(zos)
                }
            }

            if (tempFile.toFile().exists())
                tempFile.toFile().delete()

            logInfo(req, tempFile)



            val attachmentUploadInputStream = File(fo.toString()).inputStream()
            val attachmentHash = proxy.uploadAttachment(attachmentUploadInputStream)

            proxy.startFlow(::UploadFlow, attachmentHash)
                 .returnValue.getOrThrow()

            val model = HashMap<String, Any>()
            model["status"] = "Файл загружен"
            freeMarkerEngine.render(ModelAndView(model, "SparkUpload.ftl"))
        }


    }

    fun initFreemarker(resourceLoaderClass: Class<*>): FreeMarkerEngine
    {
        val freeMarkerEngine = FreeMarkerEngine()
        val freeMarkerConfiguration = Configuration()
        freeMarkerConfiguration.setTemplateLoader(ClassTemplateLoader(resourceLoaderClass, "/templates/"))
        freeMarkerEngine.setConfiguration(freeMarkerConfiguration)

        return freeMarkerEngine
    }

    fun setConnection(hostAndPort: String, username: String = "user1", password: String = "test"): CordaRPCOps
    {
        val nodeAddress = NetworkHostAndPort.parse(hostAndPort)
        val rpcConnection = CordaRPCClient(nodeAddress).start(username, password)

        return rpcConnection.proxy
    }

    // methods used for logging
    @Throws(IOException::class, ServletException::class)
    private fun logInfo(req: Request, tempFile: Path) {
        println("Uploaded file '" + getFileName(req.raw().getPart("uploaded_file")) + "' saved as '" + tempFile.toAbsolutePath() + "'")
    }

    private fun getFileName(part: Part): String? {
        for (cd in part.getHeader("content-disposition").split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            if (cd.trim { it <= ' ' }.startsWith("filename")) {
                return cd.substring(cd.indexOf('=') + 1).trim { it <= ' ' }.replace("\"", "")
            }
        }
        return null
    }

}