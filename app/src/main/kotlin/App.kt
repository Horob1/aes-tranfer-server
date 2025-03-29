package com.acteam.app

import javafx.animation.*
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.stage.Stage
import javafx.util.Duration
import javafx.application.Platform
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.effect.BlurType
import javafx.scene.effect.DropShadow
import javafx.scene.layout.*
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.paint.Color
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import java.io.File

class MainKt : Application() {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val logArea = TextArea()
    private val clients = mutableMapOf<String, Socket>()

    override fun start(primaryStage: Stage) {
        primaryStage.title = "AES Transfer Server"
        primaryStage.icons.add(Image(javaClass.getResourceAsStream("/logo.png")))
        primaryStage.scene = createLoginScene(primaryStage)
        primaryStage.isResizable = false
        primaryStage.isMaximized = false
        primaryStage.show()
        playMusic("/hello.mp3")
    }
    private fun playMusic(filePath: String) {
        val musicFile = javaClass.getResource(filePath)?.toExternalForm()
        if (musicFile != null) {
            val media = Media(musicFile)
            val mediaPlayer = MediaPlayer(media)
            mediaPlayer.play()
        } else {
            println("Cannot find music file!")
        }
    }

    private fun createLoginScene(stage: Stage): Scene {
        val animatedBackground = AnimatedBackground(350.0, 400.0)
        val logoImage = ImageView(Image(javaClass.getResourceAsStream("/logo.png"))).apply {
            fitWidth = 140.0
            fitHeight = 140.0
            isPreserveRatio = true
            isSmooth = true
            style = "-fx-background-color: rgba(255, 255, 255, 0.1); " +
                    "-fx-border-color: white; " +
                    "-fx-border-width: 2px; " +
                    "-fx-border-radius: 20px; " +
                    "-fx-background-radius: 20px; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0.5, 0, 4);"

            // Animation khi di chuột vào
            setOnMouseEntered {
                animateHover(this, 1.1, 20.0).play()
            }
            setOnMouseExited {
                animateHover(this, 1.0, 10.0).play()
            }
        }

        val logoContainer = StackPane(logoImage).apply {
            alignment = Pos.CENTER
            padding = Insets(30.0, 0.0, 20.0, 0.0)
        }

        val gridPane = GridPane().apply {
            hgap = 10.0
            vgap = 10.0
            padding = Insets(20.0)
            alignment = Pos.CENTER
        }

        val ipLabel = Label("IP ⚡:").apply {
            style = "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;"
        }
        val ipField = TextField("127.0.0.1").apply {
            prefWidth = 200.0
        }

        val portLabel = Label("PORT:").apply {
            style = "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;"
        }
        val portField = TextField("5000").apply {
            prefWidth = 200.0
        }

        val startButton = Button("Start Server").apply {
            minWidth = 260.0
            styleClass.add("button")
            setOnAction {
                val ip = ipField.text
                val port = portField.text.toIntOrNull() ?: return@setOnAction
                startServer(ip, port)
                stage.scene = createLogScene()
            }
        }

        val buttonContainer = HBox(startButton).apply {
            alignment = Pos.CENTER
            padding = Insets(10.0, 0.0, 20.0, 0.0)
        }

        gridPane.add(ipLabel, 0, 0)
        gridPane.add(ipField, 1, 0)
        gridPane.add(portLabel, 0, 1)
        gridPane.add(portField, 1, 1)
        gridPane.add(buttonContainer, 0, 2, 2, 1)


        val layout = StackPane(animatedBackground, VBox(logoContainer, gridPane).apply {
            alignment = Pos.CENTER
        })

        val scene = Scene(layout, 350.0, 400.0)
        scene.stylesheets.add(javaClass.getResource("/styles.css")?.toExternalForm())
        return scene
    }

    // Hiệu ứng scale khi hover
    private fun animateHover(node: ImageView, scaleFactor: Double, shadowSize: Double): ParallelTransition {
        val scaleTransition = ScaleTransition(Duration.millis(250.0), node).apply {
            toX = scaleFactor
            toY = scaleFactor
            interpolator = Interpolator.EASE_OUT
        }

        // Hiệu ứng sáng bóng hơn
        val baseHue = 220.0 // Màu xanh dương chủ đạo
        val glowColor = Color.hsb(baseHue, 0.6, 1.0) // Giữ màu sáng bóng

        val glowEffect = DropShadow(BlurType.GAUSSIAN, glowColor, shadowSize, 0.2, 0.0, 4.0)
        val glowAnimation = Timeline(
            KeyFrame(Duration.millis(250.0), KeyValue(node.effectProperty(), glowEffect, Interpolator.EASE_OUT))
        )

        return ParallelTransition(scaleTransition, glowAnimation)
    }


    private fun createLogScene(): Scene {
        val animatedBackground = AnimatedBackground(500.0, 400.0)

        val logLabel = Label("📜 Server Logs:").apply {
            styleClass.add("log-label")
        }

        logArea.apply {
            styleClass.add("log-area")
            isEditable = false
            isWrapText = true
            prefHeight = 200.0
        }

        val stopIcon = ImageView(Image(javaClass.getResourceAsStream("/stop.png"))).apply {
            fitWidth = 20.0
            fitHeight = 20.0
        }

        val stopButton = Button("Stop Server", stopIcon).apply {
            styleClass.add("stop-button")
            minWidth = 232.0
            setOnAction { stopServer() }
        }

        val clearIcon = ImageView(Image(javaClass.getResourceAsStream("/clean.png"))).apply {
            fitWidth = 20.0
            fitHeight = 20.0
        }

        val clearLogButton = Button("Clear Log", clearIcon).apply {
            styleClass.add("clear-button")
            minWidth = 232.0
            setOnAction { logArea.text = "" }
        }

        val buttonBox = HBox(10.0, clearLogButton, stopButton).apply {
            alignment = Pos.CENTER
            padding = Insets(10.0, 0.0, 10.0, 0.0)
        }

        val content = VBox(10.0, logLabel, logArea, buttonBox).apply {
            padding = Insets(20.0)
            alignment = Pos.CENTER
            maxWidth = 450.0
        }

        val layout = StackPane(animatedBackground, content)
        val scene = Scene(layout, 500.0, 400.0)

        // Thêm CSS vào scene
        scene.stylesheets.add(javaClass.getResource("/styles2.css")?.toExternalForm())

        return scene
    }

    private fun startServer(ip: String, port: Int) {
        thread {
            try {
                serverSocket = ServerSocket(port)
                isRunning = true
                Platform.runLater { appendLog("Server running on $ip:$port") }

                while (isRunning) {
                    val clientSocket = serverSocket?.accept() ?: break
                    thread { handleClient(clientSocket) }
                }
            } catch (e: IOException) {
                Platform.runLater { appendLog("Error starting server: ${e.message}") }
            }
        }
    }

    private fun stopServer() {
        isRunning = false
        serverSocket?.close()
        appendLog("Server stopped")
        Platform.runLater {
            val stage = logArea.scene.window as Stage
            stage.scene = createLoginScene(stage)
        }
    }

    private fun handleClient(socket: Socket) {
        var username: String? = null
        try {

            val input = DataInputStream(socket.getInputStream())
            val output = DataOutputStream(socket.getOutputStream())

            while (true) {
                val message = input.readUTF().trim()

                // Xử lý đăng nhập
                if (message.startsWith("LOGIN:")) {
                    username = message.removePrefix("LOGIN:").trim()

                    // Kiểm tra username hợp lệ
                    if (username.isEmpty()) {
                        output.writeUTF("ERROR: Invalid username!")
                        continue
                    }
                    if (clients.containsKey(username)) {
                        output.writeUTF("ERROR: Username already exists!")
                        continue
                    }

                    clients[username] = socket
                    appendLog("✅ $username is online!")
                    output.writeUTF("OK: Login successful!")
                    continue
                }

                // Danh sách client
                if (message == "LIST_CLIENTS") {
                    val clientList = clients.keys.joinToString(",")
                    output.writeUTF(clientList.ifEmpty { "NO_CLIENTS" })
                    continue
                }

                // Nhận file từ client
                if (message == "START_FILE") {
                    val receiver = input.readUTF().trim()
                    val fileName = input.readUTF().trim()
                    val fileSize = input.readLong()

                    val serverSaveDir = File("Server_save").apply { mkdirs() }
                    val pendingFile = File(serverSaveDir, fileName)

                    appendLog("📥 Receiving file: $fileName ($fileSize bytes)")

                    FileOutputStream(pendingFile).use { fos ->
                        val buffer = ByteArray(65536) // 64KB buffer giúp giảm số lần đọc
                        var totalRead: Long = 0

                        while (totalRead < fileSize) {
                            val bytesRead = input.read(buffer)
                            if (bytesRead == -1) break
                            fos.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                        }
                    }

                    // Kiểm tra xem đã nhận đủ file chưa
                    if (pendingFile.length() == fileSize) {
                        appendLog("✅ File $fileName received successfully!")
                        output.writeUTF("OK: File $fileName received!")
                        sendFileToReceiver(receiver, pendingFile)
                    } else {
                        appendLog("⚠️ File $fileName may be corrupted! Expected $fileSize bytes, got ${pendingFile.length()} bytes")
                        output.writeUTF("ERROR: File transfer incomplete!")
                    }
                }
            }
        } finally {
            username?.let {
                if (clients.remove(it) != null) {
                    appendLog("🔴 $it is offline")
                }
            }

            try {
                socket.close()
            } catch (e: IOException) {
                appendLog("⚠️ Error closing socket: ${e.message}")
            }
        }
    }


    private fun sendFileToReceiver(receiver: String, file: File) {
        clients[receiver]?.let { receiverSocket ->
            try {
                val output = DataOutputStream(receiverSocket.getOutputStream())

                output.writeUTF("FILE")
                output.writeUTF(file.name)
                output.writeLong(file.length())

                FileInputStream(file).use { fileIn ->
                    val buffer = ByteArray(65536) // Tăng buffer lên 64KB
                    var bytesRead: Int
                    while (fileIn.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
                output.flush() // Đảm bảo tất cả dữ liệu được gửi đi

                Platform.runLater { appendLog("📤 File ${file.name} sent to $receiver") }
            } catch (e: IOException) {
                appendLog("❌ Error sending file to $receiver: ${e.message}")
            }
        } ?: run {
            appendLog("⚠️ $receiver is offline, file not sent.")
        }
    }



    private fun appendLog(text: String) {
        when {
            text.contains("online") -> playMusic("/hello.mp3")
            text.contains("offline") -> playMusic("/bye.mp3")
            else -> playMusic("/noti.mp3")
        }
        val currentTime = java.util.Date()
        val formattedTime = java.text.SimpleDateFormat("HH:mm:ss").format(currentTime)
        Platform.runLater { logArea.appendText("[$formattedTime] $text\n") }
    }

}

fun main() {
    Application.launch(MainKt::class.java)
}
