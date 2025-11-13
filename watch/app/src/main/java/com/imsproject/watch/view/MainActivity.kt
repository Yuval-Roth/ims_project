package com.imsproject.watch.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.VibrationEffect
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.PickerState
import androidx.wear.compose.material.rememberPickerState
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.BLUE_COLOR
import com.imsproject.watch.COLUMN_PADDING
import com.imsproject.watch.DARK_BACKGROUND_COLOR
import com.imsproject.watch.FIRST_QUESTION
import com.imsproject.watch.GRASS_GREEN_COLOR
import com.imsproject.watch.LIGHT_BLUE_COLOR
import com.imsproject.watch.MENU_COLOR
import com.imsproject.watch.R
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.SECOND_QUESTION
import com.imsproject.watch.SILVER_COLOR
import com.imsproject.watch.TEXT_SIZE
import com.imsproject.watch.initProperties
import com.imsproject.watch.model.REST_SCHEME
import com.imsproject.watch.model.SERVER_IP
import com.imsproject.watch.textStyle
import com.imsproject.watch.utils.ErrorReporter
import com.imsproject.watch.utils.QRGenerator
import com.imsproject.watch.view.contracts.FlourMillResultContract
import com.imsproject.watch.view.contracts.FlowerGardenResultContract
import com.imsproject.watch.view.contracts.PacmanResultContract
import com.imsproject.watch.view.contracts.RecessResultContract
import com.imsproject.watch.view.contracts.Result
import com.imsproject.watch.view.contracts.TreeResultContract
import com.imsproject.watch.view.contracts.WaterRipplesResultContract
import com.imsproject.watch.view.contracts.WavesResultContract
import com.imsproject.watch.view.contracts.WineGlassesResultContract
import com.imsproject.watch.viewmodel.MainViewModel
import com.imsproject.watch.viewmodel.MainViewModel.State
import com.imsproject.watch.viewmodel.gesturepractice.FlourMillGesturePracticeViewModel
import com.imsproject.watch.viewmodel.gesturepractice.FlowerGardenGesturePracticeViewModel
import com.imsproject.watch.viewmodel.gesturepractice.PacmanGesturePracticeViewModel
import com.imsproject.watch.viewmodel.gesturepractice.TreeGesturePracticeViewModel
import com.imsproject.watch.viewmodel.gesturepractice.WaterRipplesGesturePracticeViewModel
import com.imsproject.watch.viewmodel.gesturepractice.WavesGesturePracticeViewModel
import com.imsproject.watch.viewmodel.gesturepractice.WineGlassesGesturePracticeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private val viewModel : MainViewModel by viewModels<MainViewModel>()
    private lateinit var waterRipples: ActivityResultLauncher<Map<String,Any>>
    private lateinit var wineGlasses: ActivityResultLauncher<Map<String,Any>>
    private lateinit var flourMill: ActivityResultLauncher<Map<String,Any>>
    private lateinit var flowerGarden: ActivityResultLauncher<Map<String,Any>>
    private lateinit var pacman: ActivityResultLauncher<Map<String,Any>>
    private lateinit var waves: ActivityResultLauncher<Map<String,Any>>
    private lateinit var recess: ActivityResultLauncher<Map<String,Any>>
    private lateinit var tree: ActivityResultLauncher<Map<String,Any>>
    private lateinit var wifiLock: WifiManager.WifiLock
    private val idsList = listOf("0","1","2","3","4","5","6","7","8","9")
    private var viewModelsInitialized = false


    // Gesture practice view models
    private val waterRipplesGesturePracticeViewModel by viewModels<WaterRipplesGesturePracticeViewModel>()
    private val flowerGardenGesturePracticeViewModel by viewModels<FlowerGardenGesturePracticeViewModel>()
    private val wineGlassesGesturePracticeViewModel by viewModels<WineGlassesGesturePracticeViewModel>()
    private val flourMillGesturePracticeViewModel by viewModels<FlourMillGesturePracticeViewModel>()
    private val pacmanGesturePracticeViewModel by viewModels<PacmanGesturePracticeViewModel>()
    private val wavesGesturePracticeViewModel by viewModels<WavesGesturePracticeViewModel>()
    private val treeGesturePracticeViewModel by viewModels<TreeGesturePracticeViewModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val metrics = getSystemService(WindowManager::class.java).currentWindowMetrics
        initProperties(metrics.bounds.width())
        registerActivities()
        setupSensorsPermission()
        setupWifi()
        setupUncaughtExceptionHandler()
        viewModel.onCreate(applicationContext)
        setContent {
            MaterialTheme {
//                viewModel.setState(State.SENSOR_CHECK)
//                MainViewModel::class.java.getDeclaredField("_isWarmup").apply {
//                    isAccessible = true
//                    val fieldValue = get(viewModel) as MutableStateFlow<Boolean>
//                    fieldValue.value = false
//                }
//                MainViewModel::class.java.getDeclaredField("_gameType").apply {
//                    isAccessible = true
//                    val fieldValue = get(viewModel) as MutableStateFlow<GameType?>
//                    fieldValue.value = GameType.PACMAN
//                }
//                MainViewModel::class.java.getDeclaredField("_expId").apply {
//                    isAccessible = true
//                    val fieldValue = get(viewModel) as MutableStateFlow<String?>
//                    fieldValue.value = "exp123"
//                }
//                AfterGameQuestions()
//                UploadingScreen("שומר נתונים....", 500, 1000)
                Main()
//                ColorConfirmationScreen(
//                    MainViewModel.PlayerColor.BLUE
//                ) { }
//                initGesturePracticeViewModels(MainViewModel.PlayerColor.BLUE)
//                GesturePractice(GameType.WINE_GLASSES) { }
//                AfterExperiment("exp123","123")
//                ActivityReminder(GameType.WATER_RIPPLES) { }
//                CountdownToGame(true,5) { }
//                ColorConfirmationScreen(MainViewModel.PlayerColor.BLUE){}
            }
        }
    }

    private fun resetGesturePracticeViewModels(){
        if(viewModelsInitialized){
            waterRipplesGesturePracticeViewModel.reset()
            flowerGardenGesturePracticeViewModel.reset()
            wineGlassesGesturePracticeViewModel.reset()
            flourMillGesturePracticeViewModel.reset()
            pacmanGesturePracticeViewModel.reset()
            wavesGesturePracticeViewModel.reset()
            treeGesturePracticeViewModel.reset()
        }
    }

    private fun initGesturePracticeViewModels(playerColor: MainViewModel.PlayerColor) {
        if (!viewModelsInitialized) {
            waterRipplesGesturePracticeViewModel.init(applicationContext, playerColor)
            flowerGardenGesturePracticeViewModel.init(applicationContext, playerColor)
            wineGlassesGesturePracticeViewModel.init(applicationContext, playerColor)
            flourMillGesturePracticeViewModel.init(applicationContext, playerColor)
            pacmanGesturePracticeViewModel.init(applicationContext, playerColor)
            wavesGesturePracticeViewModel.init(applicationContext, playerColor)
            treeGesturePracticeViewModel.init(applicationContext, playerColor)
            viewModelsInitialized = true
        }
    }

    private fun setupUncaughtExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            ErrorReporter.report(e)
            viewModel.fatalError(
                """
                Uncaught Exception in thread ${t.name}:
                ${e.stackTraceToString()}
                """.trimIndent()
            )
        }
    }

    override fun onDestroy() {
        viewModel.onDestroy()
        wifiLock.release()
        super.onDestroy()
    }

    private fun setupWifi() {
        val wifiManager = getSystemService(WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_LOW_LATENCY, "imsproject")
        wifiLock.acquire()
    }

    private fun afterGame(result: Result) {
        setupUncaughtExceptionHandler()
        viewModel.afterGame(result)
    }

    private fun registerActivities(){
        waterRipples = registerForActivityResult(WaterRipplesResultContract()) { afterGame(it) }
        wineGlasses = registerForActivityResult(WineGlassesResultContract()) { afterGame(it) }
        flourMill = registerForActivityResult(FlourMillResultContract()) { afterGame(it) }
        flowerGarden = registerForActivityResult(FlowerGardenResultContract()) { afterGame(it) }
        pacman = registerForActivityResult(PacmanResultContract()) { afterGame(it) }
        waves = registerForActivityResult(WavesResultContract()) { afterGame(it) }
        recess = registerForActivityResult(RecessResultContract()) { afterGame(it) }
        tree = registerForActivityResult(TreeResultContract()) { afterGame(it) }
    }

    private fun setupSensorsPermission() {
        if (ActivityCompat.checkSelfPermission(applicationContext, applicationContext.getString(R.string.BodySensors)) == PackageManager.PERMISSION_DENIED){
            requestPermissions(arrayOf(Manifest.permission.BODY_SENSORS), 0)
        }
    }

    @Composable
    private fun Main() {

        val state by viewModel.state.collectAsState()
        val loading by viewModel.loading.collectAsState()
        val reconnecting by viewModel.reconnecting.collectAsState()

        when (state) {

            // ====================== PRE-EXPERIMENT FLOW STATES ====================== |

            State.DISCONNECTED -> {
                BlankScreen()
                viewModel.connect()
            }

            State.SELECTING_ID -> PickingIdScreen {
                viewModel.enter(it)
            }

            State.ALREADY_CONNECTED -> {
                AlreadyConnectedScreen(
                    onConfirm = { viewModel.enter(viewModel.temporaryPlayerId, true) },
                    onReject = { viewModel.setState(State.SELECTING_ID) }
                )
            }

            State.CONNECTING -> LoadingScreen("מתחבר...")

            State.CONNECTED_NOT_IN_LOBBY -> {
                val userId = viewModel.playerId.collectAsState().value
                ConnectedNotInLobbyScreen(userId)
            }

            State.CONNECTED_IN_LOBBY -> {
                val userId = viewModel.playerId.collectAsState().value
                val lobbyId = viewModel.lobbyId.collectAsState().value
                ConnectedInLobbyScreen(userId, lobbyId)
            }

            // ====================== EXPERIMENT FLOW STATES ====================== |

            State.WELCOME_SCREEN -> WelcomeScreen {
                resetGesturePracticeViewModels()
                viewModel.setState(State.SENSOR_CHECK)
            }

            State.SENSOR_CHECK -> {
                val sensorHandler = viewModel.heartRateSensorHandler
                val hr = sensorHandler.heartRate.collectAsState().value
//                val ibi = sensorHandler.ibi.collectAsState().value
                val hrSensorReady = hr != 0
                SensorCheck(hrSensorReady) {
                    viewModel.setState(State.WAITING_FOR_WELCOME_SCREEN_NEXT)
                    viewModel.toggleReady()
                }
            }

            State.WAITING_FOR_WELCOME_SCREEN_NEXT,
            State.WAITING_FOR_GESTURE_PRACTICE_FINISH,
            State.WAITING_FOR_ACTIVITY_DESCRIPTION_CONFIRMATION,
            State.WAITING_FOR_RECESS -> {
                LoadingScreen("ממתין לשותף מרוחק...")
            }

            State.COLOR_CONFIRMATION -> {
                val playerColor = viewModel.myColor.collectAsState().value
                ColorConfirmationScreen(playerColor) {
                    initGesturePracticeViewModels(playerColor)
                    viewModel.setState(State.ACTIVITY_DESCRIPTION)
                }
            }

            State.ACTIVITY_DESCRIPTION -> {
                val index = viewModel.activityIndex.collectAsState().value
                val gameType = viewModel.gameType.collectAsState().value
                    ?: throw IllegalStateException("gameType is null")
                val warmup = viewModel.isWarmup.collectAsState().value
                ActivityDescription(gameType, index) {
                    if (warmup) {
                        viewModel.setState(State.ACTIVITY_REMINDER)
                    } else {
                        viewModel.setState(State.WAITING_FOR_ACTIVITY_DESCRIPTION_CONFIRMATION)
                        viewModel.toggleReady()
                    }
                }
            }

            State.ACTIVITY_REMINDER -> {
                val gameType = viewModel.gameType.collectAsState().value
                    ?: throw IllegalStateException("gameType is null")
                ActivityReminder(gameType) {
                    viewModel.setState(State.GESTURE_PRACTICE)
                }
            }

            State.GESTURE_PRACTICE -> {
                val gameType = viewModel.gameType.collectAsState().value
                    ?: throw IllegalStateException("gameType is null")
                GesturePractice(gameType) {
                    viewModel.setState(State.WAITING_FOR_GESTURE_PRACTICE_FINISH)
                    viewModel.toggleReady()
                }
            }

            State.COUNTDOWN_TO_GAME -> {
                val warmup = viewModel.isWarmup.collectAsState().value
                val countdownTimer = viewModel.countdownTimer.collectAsState().value
                CountdownToGame(warmup, countdownTimer) {
                    viewModel.setState(State.LOADING_GAME)
                    viewModel.toggleReady()
                }
            }

            State.LOADING_GAME -> LoadingScreen("טוען...")

            State.IN_GAME -> {
                BlankScreen()
                LaunchedEffect(Unit) {
                    var requiredParams = listOf(
                        viewModel.gameType.value,
                        viewModel.gameDuration.value,
                    )
                    if (requiredParams.any { it == null }) {
                        Log.e(TAG, "Missing session data, requesting lobby reconfiguration")
                    }
                    var tries = 0
                    while (requiredParams.any { it == null } && tries < 50) { // try for 5 seconds
                        viewModel.requestLobbyReconfiguration()
                        delay(100)
                        requiredParams = listOf(
                            viewModel.gameType.value,
                            viewModel.gameDuration.value,
                        )
                        if (!requiredParams.any { it == null }) {
                            Log.d(TAG, "Successfully reconfigured lobby")
                        } else {
                            tries++
                            Log.e(TAG, "Lobby reconfiguration failed, retrying")
                        }
                    }
                    if (requiredParams.any { it == null }) {
                        viewModel.fatalError("Failed to reconfigure lobby")
                        return@LaunchedEffect
                    }

                    viewModel.clearCallbacks()

                    val input = mutableMapOf<String, Any>(
                        "gameDuration" to (viewModel.gameDuration.value ?: -1),
                        "timeServerStartTime" to viewModel.gameStartTime.value,
                        "additionalData" to viewModel.additionalData.value,
                        "syncTolerance" to (viewModel.syncTolerance.value ?: -1L),
                        "syncWindowLength" to (viewModel.syncWindowLength.value ?: -1L)
                    )
                    when (val gameType = viewModel.gameType.value) {
                        GameType.WATER_RIPPLES -> waterRipples.launch(input)
                        GameType.WINE_GLASSES -> wineGlasses.launch(input)
                        GameType.FLOUR_MILL -> flourMill.launch(input)
                        GameType.FLOWER_GARDEN -> flowerGarden.launch(input)
                        GameType.PACMAN -> pacman.launch(input)
                        GameType.WAVES -> waves.launch(input)
                        GameType.RECESS -> recess.launch(input)
                        GameType.TREE -> tree.launch(input)
                        else -> {
                            viewModel.fatalError("Unknown game type: $gameType")
                            ErrorReporter.report(null, "Unknown game type\n${gameType}")
                        }
                    }
                }
            }

            State.UPLOADING_EVENTS -> {
                val bytesSent = viewModel.bytesSent.collectAsState().value
                    val totalBytes = viewModel.totalBytes.collectAsState().value
                UploadingScreen("שומר נתונים....", bytesSent, totalBytes)
            }

            State.AFTER_GAME_QUESTIONS -> AfterGameQuestions()

            State.AFTER_GAME_WAITING -> LoadingScreen("טוען...")

            State.AFTER_EXPERIMENT -> {
                val userId = viewModel.playerId.collectAsState().value
                val expId = viewModel.expId.collectAsState().value
                    ?: throw IllegalStateException("expId is null")
                AfterExperiment(expId, userId)
            }

            State.THANKS_FOR_PARTICIPATING -> ThanksForParticipating()

            State.ERROR -> {
                val error = viewModel.error.collectAsState().value ?: "No error message"
                ErrorScreen(error) {
                    viewModel.clearError()
                }
            }

            State.CONNECTION_LOST -> ConnectionLost()
        }

        if (loading) {
            FloatingLoading()
        }
        if (reconnecting) {
            ReconnectingOverlay {
                viewModel.connectionLost()
            }
        }
    }

    @Composable
    fun AlreadyConnectedScreen(onConfirm: () -> Unit, onReject: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DARK_BACKGROUND_COLOR),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(
                        top = COLUMN_PADDING,
                        start = COLUMN_PADDING,
                        end = COLUMN_PADDING,
                        bottom = COLUMN_PADDING / 2
                    )
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ){
                RTLText("המזהה שבחרת כבר מחובר ממקום אחר.\n\nהאם תרצה/י להתחבר בכל זאת?")
                Spacer(modifier = Modifier.height((SCREEN_RADIUS*0.1f).dp))
                Row(
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        modifier = Modifier
                            .size((SCREEN_RADIUS*0.22f).dp),
                        onClick = { onReject() },
                        shape = CircleShape,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "reject"
                        )
                    }
                    Spacer(modifier = Modifier.width((SCREEN_RADIUS*0.12f).dp))
                    Button(
                        modifier = Modifier
                            .size((SCREEN_RADIUS*0.22f).dp),
                        onClick = { onConfirm() },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "confirm"
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun PickingIdScreen(onClick : (String) -> Unit) {
        val scope = rememberCoroutineScope()
        val leftNum = rememberPickerState(10, 0)
        val middleNum = rememberPickerState(10, 0)
        val rightNum = rememberPickerState(10, 0)
        val getId = remember { getId@{
            return@getId idsList[leftNum.selectedOption] +
                    idsList[middleNum.selectedOption] +
                    idsList[rightNum.selectedOption]
        }}

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DARK_BACKGROUND_COLOR),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(
                        top = COLUMN_PADDING / 4,
                        bottom = COLUMN_PADDING / 2,
                        start = COLUMN_PADDING / 2,
                        end = COLUMN_PADDING / 2
                    )
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .background(LIGHT_BLUE_COLOR)
                        .fillMaxWidth()
                        .fillMaxHeight(0.125f)
                    ,
                    contentAlignment = Alignment.Center,
                ){
                    RTLText(
                        text = "מזהה משתתף",
                        style = textStyle.copy(color = Color.Black),
                    )
                }

                Spacer(modifier = Modifier.height((SCREEN_RADIUS*0.08f).dp))
                Row {
                    SimplePicker(state = leftNum, items = idsList)
                    Spacer(modifier = Modifier.width((SCREEN_RADIUS*0.14f).dp))
                    SimplePicker(state = middleNum, items = idsList)
                    Spacer(modifier = Modifier.width((SCREEN_RADIUS*0.14f).dp))
                    SimplePicker(state = rightNum, items = idsList)
                }
                Spacer(modifier = Modifier.height((SCREEN_RADIUS*0.08f).dp))
                Row(
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        modifier = Modifier
                            .size((SCREEN_RADIUS*0.22f).dp),
                        onClick = {
                            // reset to 000
                            scope.launch { leftNum.animateScrollToOption(0) }
                            scope.launch { middleNum.animateScrollToOption(0) }
                            scope.launch { rightNum.animateScrollToOption(0) }
                        },
                        shape = CircleShape,
                        enabled = getId() != "000"
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "reset"
                        )
                    }
                    Spacer(modifier = Modifier.width((SCREEN_RADIUS*0.12f).dp))
                    Button(
                        modifier = Modifier
                            .size((SCREEN_RADIUS*0.22f).dp),
                        onClick = {
                            onClick(getId())
                        },
                        enabled = getId() != "000"
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Next"
                        )
                    }
                }
            }
        }

    }

    @Composable
    private fun ConnectedNotInLobbyScreen(id: String) {
        ButtonedPage(
            buttonText = "החלף מזהה",
            onClick = { viewModel.exit() },
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.fillMaxHeight(0.3f))
                RTLText("המזהה שלי: $id")
                Spacer(modifier = Modifier.fillMaxHeight(0.35f))
                RTLText("מחכה לצירוף ללובי...")
            }
        }
    }

    @Composable
    private fun ConnectedInLobbyScreen(
        userId: String,
        lobbyId: String
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DARK_BACKGROUND_COLOR)
                .padding(bottom = (SCREEN_RADIUS * 0.08f).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ){
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f),
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                Spacer(modifier = Modifier.fillMaxHeight(0.3f))
                RTLText(
                    text = "מזהה משתתף: $userId",
                    style = textStyle,
                )
                Spacer(modifier = Modifier.height(3.dp))
                RTLText(
                    text = "מזהה לובי: $lobbyId",
                    style = textStyle,
                )
                Spacer(modifier = Modifier.fillMaxHeight(0.3f))
                RTLText(
                    text = "ממתין להתחלת הניסוי...",
                    style = textStyle,
                )
            }
        }
    }

    @Composable
    fun WelcomeScreen(onClick : () -> Unit) {
        ButtonedPage(
            buttonText = "אני מוכנ/ה להתחיל",
            onClick = onClick,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.fillMaxHeight(0.5f))
                RTLText(
                    text = "ברוכים הבאים לניסוי!",
                    style = textStyle.copy(fontSize = TEXT_SIZE * 1.5f),
                )
            }
        }
    }

    @Composable
    fun SensorCheck(hrSensorReady: Boolean, onConfirm: () -> Unit){
        val ready = hrSensorReady || viewModel.heartRateUnavailable.collectAsState().value
        ButtonedPage(
            buttonText = "המשך",
            onClick = onConfirm,
            disableButton = false // !ready //TODO: uncomment to enforce sensor check
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val text = buildAnnotatedString {
                    append("בודק מנח שעון על היד...\n\n")
                    if(ready){
                        withStyle(style = SpanStyle(color = GRASS_GREEN_COLOR)){
                            append("תקין!")
                        }
                    }
                }
                Spacer(modifier = Modifier.fillMaxHeight(0.3f))
                RTLText(text)
            }
        }
    }

    @Composable
    fun ColorConfirmationScreen(myColor: MainViewModel.PlayerColor, onConfirm: () -> Unit) {
        ButtonedPage(
            buttonText = "הבנתי",
            onClick = onConfirm
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val ellipseShape = GenericShape { size, _ ->
                    addOval(Rect(0f, 0f, size.width, size.height))
                }
                val myText = buildAnnotatedString {
                    when(myColor) {
                        MainViewModel.PlayerColor.BLUE ->
                            withStyle(style = SpanStyle(color = Color(0xff0e44c1))) {
                                append("את/ה תהיה בצבע כחול")
                            }
                        MainViewModel.PlayerColor.GREEN ->
                            withStyle(style = SpanStyle(color = Color(0xff335f36))) {
                                append("את/ה תהיה בצבע ירוק")
                            }
                    }
                }
                val otherText = buildAnnotatedString {
                    when(myColor) {
                        MainViewModel.PlayerColor.BLUE ->
                            withStyle(style = SpanStyle(color = GRASS_GREEN_COLOR)) {
                                append("והשותף יהיה בצבע ירוק")
                            }
                        MainViewModel.PlayerColor.GREEN ->
                            withStyle(style = SpanStyle(color = BLUE_COLOR)) {
                                append("והשותף יהיה בצבע כחול")
                            }
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((SCREEN_RADIUS * 0.16f).dp)
                        .background(color = MENU_COLOR),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ){
                    Spacer(modifier = Modifier.fillMaxHeight(0.42f))
                    RTLText(
                        text = "שותף מרוחק מחובר",
                        style = textStyle.copy(color = Color.Black),
                    )
                }
                Spacer(modifier = Modifier.fillMaxHeight(0.05f))
                RTLText(
                    text = "בכל הניסוי,",
                    style = textStyle.copy(fontSize = TEXT_SIZE * 1f),
                )
                Spacer(modifier = Modifier.fillMaxHeight(0.1f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .fillMaxHeight(0.6f)
                        .clip(ellipseShape)
                        .background(color = if (myColor == MainViewModel.PlayerColor.BLUE) BLUE_COLOR else GRASS_GREEN_COLOR),
                    contentAlignment = Alignment.Center
                ){
                    RTLText(
                        text = myText,
                        style = textStyle.copy(fontSize = TEXT_SIZE * 1f),
                    )
                }
                Spacer(modifier = Modifier.fillMaxHeight(0.1f))
                RTLText(
                    text = otherText,
                    style = textStyle.copy(fontSize = TEXT_SIZE * 1f),
                )
            }
        }
    }

    @Composable
    fun ActivityDescription(gameType: GameType, activityIndex: Int, onConfirm: () -> Unit) {
        ButtonedPage(
            buttonText = "המשך",
            onClick = onConfirm,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.fillMaxHeight(0.3f))
                RTLText(
                    text = "פעילות #$activityIndex",
                    style = textStyle.copy(fontSize = TEXT_SIZE * 1.2f, textDecoration = TextDecoration.Underline),
                )
                Spacer(modifier = Modifier.fillMaxHeight(0.2f))
                RTLText(
                    text = gameType.hebrewName(),
                    style = textStyle.copy(fontSize = TEXT_SIZE * 1.5f),
                )
            }
        }
    }

    @Composable
    fun ActivityReminder(gameType: GameType, onConfirm: () -> Unit) {
        ButtonedPage(
            buttonText = "המשך",
            onClick = onConfirm,
        ) {
            val text = when(gameType){
                GameType.WATER_RIPPLES -> """
                    בפעילות זו הקשות משותפות
                    יצרו אדוות מים וצליל
                """
                GameType.WINE_GLASSES -> """
                    בפעילות זו סיבוב משותף
                    על שפת השעון יצור צליל תהודה
                """
                GameType.FLOUR_MILL -> """
                    בפעילות זו סיבוב משותף
                    של ציר המטחנה יצור קמח
                """
                GameType.FLOWER_GARDEN -> """
                    בפעילות זו הקשות משותפות
                    ישתלו פרחים
                """
                GameType.WAVES -> """
                    בפעילות זו נמסור גלים
                    מצד לצד לסירוגין
                """
                GameType.TREE -> """
                    בפעילות זו מסירת שמש
                    ומים לסירוגין יצמיחו עץ
                """
                GameType.PACMAN -> """
                    להשלים אם צריך
                """
                else -> throw IllegalStateException("Unknown game type")
            }.trimIndent()
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.fillMaxHeight(0.1f))
                RTLText(
                    text = gameType.hebrewName(),
                    style = textStyle.copy(fontSize = TEXT_SIZE, textDecoration = TextDecoration.Underline),
                )
                Spacer(modifier = Modifier.fillMaxHeight(0.2f))
                RTLText(text = "תזכורת:\n$text")
            }
        }
    }

    @Composable
    fun GesturePractice(gameType: GameType, onComplete: () -> Unit) {
        var showOverlay by remember { mutableStateOf(true) }
        var done: Boolean
        val viewModel = when(gameType) {
            GameType.WATER_RIPPLES -> waterRipplesGesturePracticeViewModel
            GameType.FLOWER_GARDEN -> flowerGardenGesturePracticeViewModel
            GameType.WINE_GLASSES -> wineGlassesGesturePracticeViewModel
            GameType.FLOUR_MILL -> flourMillGesturePracticeViewModel
            GameType.PACMAN -> pacmanGesturePracticeViewModel
            GameType.WAVES -> wavesGesturePracticeViewModel
            GameType.TREE -> treeGesturePracticeViewModel
            else -> throw IllegalStateException("No gesture practice defined for game type $gameType")
        }
        done = viewModel.done.collectAsState().value
        viewModel.RunGesturePractice(3000)
        if(showOverlay){
            val (headline, body) = when(gameType){
                GameType.WATER_RIPPLES,GameType.FLOWER_GARDEN -> "תרגול הקשה" to """
                    במסך הבא נתרגל הקשה.
                    ההקשה מתבצעת במרכז המסך
                """.trimIndent()
                GameType.WINE_GLASSES, GameType.FLOUR_MILL -> "תרגול סיבוב" to """
                    במסך הבא נתרגל סיבוב.
                    הסיבוב מתבצע קרוב למסגרת
                    של השעון
                """.trimIndent()
                GameType.WAVES,GameType.TREE, GameType.PACMAN ->"תרגול מסירה" to """
                    במסך הבא נתרגל מסירה.
                    פעולת המסירה מתבצעת ממסגרת
                    השעון פנימה
                """.trimIndent()
                else -> throw IllegalStateException("Unknown game type")
            }
            ButtonedPage(
                modifier = Modifier.disableClicks(),
                buttonText = "המשך",
                onClick = { showOverlay = false },
                backgroundColor = DARK_BACKGROUND_COLOR
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    Spacer(modifier = Modifier.fillMaxHeight(0.1f))
                    RTLText(gameType.hebrewName(), style = textStyle.copy(textDecoration = TextDecoration.Underline))
                    Spacer(modifier = Modifier.fillMaxHeight(0.2f))
                    RTLText(body)
                }
            }
        }
        if(done){
            ButtonedPage(
                modifier = Modifier.disableClicks(),
                buttonText = "המשך",
                onClick = {
                    viewModel.reset()
                    onComplete()
                },
                backgroundColor = DARK_BACKGROUND_COLOR
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ){
                    Spacer(modifier = Modifier.fillMaxHeight(0.2f))
                    RTLText("""
                        כל הכבוד!
                        נחכה לשותף ונתחיל בסבב אימון
                    """.trimIndent())
                }
            }
        }
    }

    @Composable
    fun CountdownToGame(warmup: Boolean, countDownFrom: Int, onComplete: () -> Unit) {
        Column(
            modifier = Modifier
                .background(color = DARK_BACKGROUND_COLOR)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val currentNumber = remember { MutableStateFlow(countDownFrom) }
            var alpha by remember { mutableFloatStateOf(1f) }
            val countdown = remember { Animatable(countDownFrom.toFloat()) }
            val scope = rememberCoroutineScope()
            val clickVibration = remember {
                VibrationEffect.createOneShot(
                    100, // duration in milliseconds
                    255  // amplitude (0–255); 255 = strongest
                )
            }
            LaunchedEffect(Unit){
                scope.launch {
                    currentNumber.collect {
                        // vibrate on each number change
                        if(it <= 5){
                            viewModel.vibrator.vibrate(clickVibration)
                        }
                    }
                }
                countdown.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = countDownFrom * 1000,
                        easing = LinearEasing
                    )
                ) {
                    currentNumber.value = ceil(value).toInt()
                    alpha = value - value.toInt()
                }
                onComplete()
            }
            Spacer(modifier = Modifier.fillMaxHeight(0.3f))
            RTLText(
                text = if(warmup) "מיד מתחילים סבב אימון" else "מיד מתחילים סבב ניסוי",
                style = textStyle.copy(fontSize = TEXT_SIZE * 1.25f),
            )
            Spacer(modifier = Modifier.fillMaxHeight(0.1f))
            Text(
                modifier = Modifier.alpha(alpha),
                text = currentNumber.collectAsState().value.toString(),
                style = textStyle.copy(fontSize = TEXT_SIZE * 4f)
            )
        }

    }

    @Composable
    fun AfterGameQuestions() {
        val pageCount = remember { 2 }
        val scope = rememberCoroutineScope()
        var firstSliderValue by remember { mutableIntStateOf(-1) }
        var secondSliderValue by remember { mutableIntStateOf(-1) }
        val pagerState = rememberPagerState(pageCount = { pageCount })
        var buttonDisabled by remember { mutableStateOf(true) }

        ButtonedPage(
            buttonText = "המשך",
            onClick = {
                scope.launch {
                    val nextPage = pagerState.currentPage + 1
                    if(nextPage < pageCount){
                        buttonDisabled = true
                        pagerState.animateScrollToPage(nextPage)
                    } else {
                        viewModel.uploadAnswers(mapOf(
                            FIRST_QUESTION to firstSliderValue.toString(),
                            SECOND_QUESTION to secondSliderValue.toString()
                        ))
                    }
                }
            },
            disableButton = buttonDisabled
        ) {
            VerticalPager(
                state = pagerState,
                userScrollEnabled = false
            ){ page ->
                when(page) {
                    0 -> {
                        Column(modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = COLUMN_PADDING,
                                end = COLUMN_PADDING
                            )
                        ){
                            Spacer(Modifier.fillMaxHeight(0.25f))
                            SliderQuestion(
                                FIRST_QUESTION,
                                firstSliderValue,
                            ) {
                                firstSliderValue = it
                                buttonDisabled = false
                            }
                        }
                    }
                    1 -> {
                        Column(modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = COLUMN_PADDING * 0.75f,
                                end = COLUMN_PADDING * 0.75f
                            )
                        ) {
                            Spacer(Modifier.fillMaxHeight(0.35f))
                            SliderQuestion(SECOND_QUESTION,secondSliderValue) {
                                secondSliderValue = it
                                buttonDisabled = false
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AfterExperiment(expId: String, userId: String) {
        val pageCount = remember { 2 }
        val scope = rememberCoroutineScope()
        val pagerState = rememberPagerState(pageCount = { pageCount })
        var disableButton by remember { mutableStateOf(false) }
        ButtonedPage(
            buttonText = if(pagerState.targetPage == pageCount -1) "סיום" else "המשך",
            onClick = {
                scope.launch {
                    val nextPage = pagerState.currentPage + 1
                    if(nextPage < pageCount){
                        pagerState.animateScrollToPage(nextPage)
                    } else {
                        viewModel.setState(State.THANKS_FOR_PARTICIPATING)
                    }
                }
            },
            disableButton = disableButton
        ) {
            VerticalPager(
                state = pagerState,
                userScrollEnabled = false
            ){ page ->
                when(page) {
                    0 -> {
                        Column(modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = COLUMN_PADDING,
                                end = COLUMN_PADDING
                            ),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ){
                            Spacer(Modifier.fillMaxHeight(0.2f))
                            RTLText("שאלון סיום", style = textStyle.copy(textDecoration = TextDecoration.Underline))
                            Spacer(Modifier.fillMaxHeight(0.2f))
                            RTLText("סרקו עם הטלפון את הברקוד\nבמסך הבא כדי להציג את\nהשאלון ")
                        }
                    }
                    1 -> {
                        LaunchedEffect(expId) {
                            disableButton = true
                            delay(3000)
                            disableButton = false
                        }
                        Box(modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = COLUMN_PADDING * 0.9f,
                                bottom = COLUMN_PADDING * 0.25f
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            ExperimentQuestionsQRCode(userId,expId,Modifier.size((SCREEN_RADIUS*0.8f).dp))
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ThanksForParticipating(){
        LaunchedEffect(Unit) {
            val inLobby = viewModel.lobbyId.value != ""
            if(! inLobby){
                delay(5000)
                viewModel.setState(State.CONNECTED_NOT_IN_LOBBY)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DARK_BACKGROUND_COLOR),
            contentAlignment = Alignment.Center
        ){
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = COLUMN_PADDING,
                    end = COLUMN_PADDING
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.fillMaxHeight(0.4f))
                RTLText("תודה על השתתפותך בניסוי ! ")
                Spacer(Modifier.fillMaxHeight(0.1f))
                RTLText("נא להחזיר את השעון לנסיינים")
            }
        }
    }

    @Composable
    fun ConnectionLost(){
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DARK_BACKGROUND_COLOR),
            contentAlignment = Alignment.Center
        ){
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = COLUMN_PADDING,
                    end = COLUMN_PADDING
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.fillMaxHeight(0.4f))
                RTLText("אבד החיבור לשרת")
                Spacer(Modifier.fillMaxHeight(0.1f))
                RTLText("נא להחזיר את השעון לנסיין")
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SliderQuestion(question: String, sliderValue: Int, modifier: Modifier = Modifier, onValueChanged: (Int) -> Unit) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RTLText(
                modifier = Modifier.fillMaxWidth(),
                text = question
            )
            Spacer(modifier = Modifier.fillMaxHeight(0.1f))
            SimpleSlider(
                startingValue = sliderValue,
                onValueChange = onValueChanged,
                valueRange = 1f..7f,
                hasThumb = sliderValue > 0f
            )
            Spacer(modifier = Modifier.fillMaxHeight(0.1f))
            if(sliderValue > 0f){
                BasicText(
                    modifier = Modifier.fillMaxWidth(0.2f),
                    text = "$sliderValue",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = TEXT_SIZE*1.5,
                        textAlign = TextAlign.Center),
                )
            } else {
                RTLText(
                    modifier = Modifier.fillMaxWidth(0.6f),
                    text = "נא לבחור ערך",
                    style = TextStyle(
                        color = Color.Gray,
                        fontSize = TEXT_SIZE,
                        textAlign = TextAlign.Center),
                )
            }
        }
    }

    @Composable
    fun ExperimentQuestionsQRCode(
        pid:String,
        expId:String,
        modifier :Modifier = Modifier
    ) {
        val key = "$pid-$expId"
        val link = remember(key) { "${REST_SCHEME}://${SERVER_IP}/experiment_questions?pid=${pid}&expid=${expId}" }
        val qrBitmap = remember(key) { QRGenerator.generate(link) }
        Image(
            modifier = modifier,
            bitmap = qrBitmap,
            contentDescription = "QR Code"
        )
    }

    @Composable
    private fun SimplePicker(
        state: PickerState,
        items: List<String>
    ) {
        Picker(
            modifier = Modifier
                .size(20.dp, 75.dp)
                .background(Color.Transparent)
            ,
            state = state,
            gradientColor = DARK_BACKGROUND_COLOR,
            contentDescription = "number",
        ) {
            BasicText(
                text = items[it],
                style = TextStyle(color = Color.White, fontSize = 30.sp),
            )
        }
    }

    @Composable
    fun SimpleSlider(
        startingValue: Int,
        onValueChange: (Int) -> Unit,
        valueRange: ClosedFloatingPointRange<Float>,
        hasThumb: Boolean = true
    ) {
        val scope = rememberCoroutineScope()
        val currentValue = remember { Animatable(startingValue.toFloat())}
        val emptyBarColor = Color(0xFFE5DEEA)
        val dotsUncoveredColor = Color(0xFFAAA9AB)
        val dotsCoveredColor = Color(0xFF8468D7)
        val barColor = Color(0xFF654FA3)
        val thumbColor = Color(0xFFA585FF)
        val progress = if(currentValue.value > 0f) (currentValue.value - valueRange.start) / (valueRange.endInclusive - valueRange.start) else 0f
        fun DrawScope.drawDots() {
            val y = size.height / 2f
            val dotSpacing = size.width / (valueRange.endInclusive - 1)
            for (i in 0..< valueRange.endInclusive.toInt()) {
                val x = i * dotSpacing
                val color = if (startingValue > 0f && i / (valueRange.endInclusive.toInt() - 1f) <= progress) dotsCoveredColor else dotsUncoveredColor
                drawCircle(color, radius = (size.height / 32f).dp.toPx(), center = Offset(x, y))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val pointerEvent = awaitPointerEvent()
                            val inputChange = pointerEvent.changes.first()
                            inputChange.consume()
                            when (pointerEvent.type) {
                                PointerEventType.Press, PointerEventType.Move -> {
                                    val newValue = valueRange.start + (inputChange.position.x / size.width) * (valueRange.endInclusive - valueRange.start)
                                    val coercedValue = newValue.coerceIn(valueRange).roundToInt()
                                    scope.launch {
                                        if(currentValue.value < 0f){
                                            currentValue.snapTo(coercedValue.toFloat())
                                        } else {
                                            currentValue.animateTo(
                                                targetValue = coercedValue.toFloat(),
                                                animationSpec = tween(
                                                    durationMillis = 100,
                                                    easing = LinearEasing
                                                )
                                            )
                                        }
                                    }
                                    onValueChange(coercedValue)
                                }
                                PointerEventType.Release -> {}
                            }
                        }
                    }
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val y = size.height / 2f
                drawLine(emptyBarColor, Offset(0f, y), Offset(size.width, y), strokeWidth = (size.height/4f).dp.toPx(), cap = StrokeCap.Round)
                if (hasThumb){
                    drawLine(barColor, Offset(0f, y), Offset(size.width * progress, y), strokeWidth = (size.height/4f).dp.toPx(), cap = StrokeCap.Round)
                    drawDots()
                    drawCircle(thumbColor, radius = (size.height/4.5f).dp.toPx(), center = Offset(size.width * progress, y))
                } else {
                    drawDots()
                }
            }
        }
    }


    @Composable
    fun ScrollHintArrow(show: Boolean = true,backwards: Boolean = false) {
        val offsetY by rememberInfiniteTransition(label = "ArrowBounce").animateFloat(
            initialValue = if(backwards) -4f else 4f,
            targetValue = if(backwards) 4f else -4f,
            animationSpec = infiniteRepeatable(
                animation = tween(700),
                repeatMode = RepeatMode.Reverse
            ),
            label = "OffsetAnimation"
        )

        Box(
            modifier = Modifier
                .requiredSize((SCREEN_RADIUS * 0.15f).dp)
            ,
            contentAlignment = Alignment.TopCenter
        ){
            AnimatedVisibility(
                visible = show,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if(backwards) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Scroll",
                        modifier = Modifier
                            .requiredSize((SCREEN_RADIUS * 0.15f).dp)
                            .offset(y = offsetY.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }

    @Composable
    @SuppressLint("ModifierParameter")
    fun ButtonedPage(
        buttonText: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        textModifier: Modifier = Modifier,
        textStyle: TextStyle = com.imsproject.watch.textStyle,
        backgroundColor: Color = DARK_BACKGROUND_COLOR,
        disableButton: Boolean = false,
        content: @Composable () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = backgroundColor)
                .padding(bottom = (SCREEN_RADIUS * 0.08f).dp)
                .then(modifier)
            ,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
            ){
                content()
            }
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.55f),
                enabled = !disableButton,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = SILVER_COLOR,
                    contentColor = Color.Black
                )
            ){
                RTLText(
                    buttonText,
                    modifier = textModifier,
                    style = textStyle.copy(color = Color.Black)
                )
            }
        }
    }


    @Preview(device = "id:wearos_large_round")
    @Composable
    private fun PreviewConnectedInLobbyScreen() {
        initProperties(454)
        MaterialTheme {
            ConnectedInLobbyScreen(
                userId = "001",
                lobbyId = "0001",
//                gameType = "אדוות מים",
//                gameDuration = "60",
//                ready = true,
//                hrSensorReady = true,
            )
//            { }
        }
    }

    @Preview(device = "id:wearos_large_round")
    @Composable
    private fun PreviewConnectedNotInLobbyScreen() {
        initProperties(454)
        MaterialTheme {
            ConnectedNotInLobbyScreen(
                id = "001",
            )
        }
    }

    @Preview(device = "id:wearos_large_round")
    @Composable
    private fun PreviewButtonedPage() {
        initProperties(454)
        MaterialTheme {
            ButtonedPage("המשך", {}) { }
        }
    }
}





