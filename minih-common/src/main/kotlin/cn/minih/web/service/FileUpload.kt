package cn.minih.web.service

/**
 * 文件信息
 * @author hubin
 * @since 2023-08-05 22:47:27
 * @desc
 */
@Suppress("unused")
data class FileUpload(
    var name: String,
    var uploadedFileName: String,
    var fileName: String,
    var size: Long,
    var contentType: String,
    var contentTransferEncoding: String,
    var charSet: String,
)