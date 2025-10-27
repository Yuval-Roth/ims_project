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
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.wear.compose.material.Button
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
import com.imsproject.watch.R
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.SECOND_QUESTION
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
import com.imsproject.watch.view.contracts.Result
import com.imsproject.watch.view.contracts.WaterRipplesResultContract
import com.imsproject.watch.view.contracts.WavesResultContract
import com.imsproject.watch.view.contracts.WineGlassesResultContract
import com.imsproject.watch.viewmodel.MainViewModel
import com.imsproject.watch.viewmodel.MainViewModel.State
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
    private lateinit var wifiLock: WifiManager.WifiLock
    private val idsList = listOf("0","1","2","3","4","5","6","7","8","9")


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
                Main()
            }
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
        viewModel.setState(State.UPLOADING_EVENTS)
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
    }

    private fun setupSensorsPermission() {
        if (ActivityCompat.checkSelfPermission(applicationContext, applicationContext.getString(R.string.BodySensors)) == PackageManager.PERMISSION_DENIED){
            requestPermissions(arrayOf(Manifest.permission.BODY_SENSORS), 0)
        }
    }

    @Composable
    private fun Main(){

        val state = viewModel.state.collectAsState().value
        val loading = viewModel.loading.collectAsState().value

        when(state) {
            State.DISCONNECTED ->{
                BlankScreen()
                viewModel.connect()
            }

            State.SELECTING_ID -> PickingIdScreen {
                viewModel.enter(it)
            }

            State.ALREADY_CONNECTED -> {
                AlreadyConnectedScreen (
                    onConfirm = { viewModel.enter(viewModel.temporaryPlayerId,true) },
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
                val gameType = viewModel.gameType.collectAsState().value?.hebrewName() ?: ""
                val gameDuration = (viewModel.gameDuration.collectAsState().value ?: "").toString()
                val ready = viewModel.ready.collectAsState().value
                val sensorHandler = viewModel.heartRateSensorHandler
                val hr = sensorHandler.heartRate.collectAsState().value
//                val ibi = sensorHandler.ibi.collectAsState().value
                val hrSensorReady = hr != 0
                ConnectedInLobbyScreen(userId, lobbyId)
            }

            State.WELCOME_SCREEN -> WelcomeScreen {
                viewModel.toggleReady()
                viewModel.setState(State.WAITING_FOR_OTHER_PLAYER)
            }

            State.WAITING_FOR_OTHER_PLAYER -> {
                LoadingScreen("ממתין לשותף מרוחק...")
            }

            State.REMOTE_PLAYER_READY -> RemotePlayerReady()

            State.COLOR_CONFIRMATION -> {
                val myColor = viewModel.myColor.collectAsState().value
                ColorConfirmationScreen(myColor) {

                }
            }

            State.IN_GAME -> {
                BlankScreen()
                LaunchedEffect(Unit) {
                    var gameType = viewModel.gameType.value
                    if(gameType == null){
                        Log.e(TAG,"gameType is null, requesting lobby reconfiguration")
                    }
                    var tries = 0
                    while(gameType == null && tries < 50) { // try for 5 seconds
                        viewModel.requestLobbyReconfiguration()
                        delay(100)
                        gameType = viewModel.gameType.value
                        if(gameType != null){
                            Log.d(TAG,"Successfully reconfigured lobby")
                        } else {
                            tries++
                            Log.e(TAG,"Lobby reconfiguration failed, retrying")
                        }
                    }
                    if(gameType == null){
                        viewModel.fatalError("gameType is null and failed to reconfigure lobby")
                        return@LaunchedEffect
                    }

                    viewModel.clearListeners()

                    val input = mutableMapOf<String,Any>(
                        "timeServerStartTime" to viewModel.gameStartTime.value,
                        "additionalData" to viewModel.additionalData.value,
                        "syncTolerance" to (viewModel.syncTolerance.value ?: -1L),
                        "syncWindowLength" to (viewModel.syncWindowLength.value ?: -1L)
                    )
                    when(gameType) {
                        GameType.WATER_RIPPLES -> waterRipples.launch(input)
                        GameType.WINE_GLASSES -> wineGlasses.launch(input)
                        GameType.FLOUR_MILL -> flourMill.launch(input)
                        GameType.FLOWER_GARDEN -> flowerGarden.launch(input)
                        GameType.PACMAN -> pacman.launch(input)
                        GameType.WAVES -> waves.launch(input)
                        else -> {
                            viewModel.fatalError("Unknown game type")
                            ErrorReporter.report(null,"Unknown game type\n${gameType}")
                        }
                    }
                }
            }

            State.UPLOADING_EVENTS -> LoadingScreen("מעלה אירועים....")

            State.AFTER_GAME -> AfterGame()

            State.AFTER_EXPERIMENT -> {
                val userId = viewModel.playerId.collectAsState().value
                val expId = viewModel.expId.collectAsState().value ?: throw IllegalStateException("expId is null")
                AfterExperiment(expId,userId)
            }

            State.ERROR -> {
                val error = viewModel.error.collectAsState().value ?: "No error message"
                ErrorScreen(error) {
                    viewModel.clearError()
                }
            }
        }

        if(loading) FloatingLoading()
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



//    @Composable
//    private fun ConnectedInLobbyScreen(
//        userId: String,
//        lobbyId: String,
//        gameType: String,
//        gameDuration: String,
//        ready: Boolean,
//        hrSensorReady: Boolean,
//        onReady: () -> Unit
//    ) {
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(DARK_BACKGROUND_COLOR)
//                .padding(bottom = (SCREEN_RADIUS * 0.08f).dp),
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center,
//        ){
//            Column(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .fillMaxHeight(0.8f),
//                horizontalAlignment = Alignment.CenterHorizontally
//            ){
//                Spacer(modifier = Modifier.fillMaxHeight(0.1f))
//                val white = remember { Color(0xFFFFE095) }
//                val green = remember { Color(0xFF89F55C) }
//                Box(
//                    modifier = Modifier
//                        .background(if (ready) green else white)
//                        .fillMaxWidth()
//                        .fillMaxHeight(0.20f)
//                    ,
//                    contentAlignment = Alignment.Center,
//                ){
//                    RTLText(
//                        text = "סטטוס: "+if (ready) "מוכן" else "לא מוכן",
//                        style = textStyle.copy(color = Color.Black),
//                    )
//                }
//                Spacer(modifier = Modifier.fillMaxHeight(0.1f))
//                val halfVisibleText = remember {
//                    TextStyle(
//                        color = Color(0xFF707070),
//                        fontSize = TEXT_SIZE
//                    )
//                }
//                RTLText(
//                    text = "המזהה שלי: $userId",
//                    style = halfVisibleText,
//                )
//                Spacer(modifier = Modifier.height(3.dp))
//                RTLText(
//                    text = "מזהה לובי: $lobbyId",
//                    style = halfVisibleText,
//                )
//                Spacer(modifier = Modifier.height(3.dp))
//                RTLText(
//                    text = gameType.ifBlank { "" },
//                    style = halfVisibleText
//                )
//                Spacer(modifier = Modifier.height(3.dp))
//                RTLText(
//                    text = if(gameDuration.isNotBlank()) "$gameDuration שניות" else "",
//                    style = halfVisibleText
//                )
//            }
//            Button(
//                colors = if (!hrSensorReady && !viewModel.heartRateUnavailable.collectAsState().value)
//                    ButtonDefaults.buttonColors(
//                        backgroundColor = Color(0xFF707070).copy(alpha=0.5f),
//                        contentColor = Color.White
//                    )
//                else ButtonDefaults.primaryButtonColors(),
//                onClick = { onReady() },
//                modifier = Modifier
//                    .fillMaxWidth(0.55f)
//                    .fillMaxHeight()
//            ) {
//                val blackText = remember { textStyle.copy(color = Color.Black) }
//                RTLText(
//                    text = if(ready) "השהה התחלה" else "אפשר להתחיל",
//                    style = blackText,
//                )
//            }
//        }
//    }

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
    fun RemotePlayerReady(){
        Column(
            modifier = Modifier
                .background(color = DARK_BACKGROUND_COLOR)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val currentNumber = remember { MutableStateFlow(4) }
            var alpha by remember { mutableFloatStateOf(0f) }
            val countdown = remember { Animatable(4f) }
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
                        viewModel.vibrator.vibrate(clickVibration)
                    }
                }
                countdown.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(
                        durationMillis = 4000,
                        easing = LinearEasing
                    )
                ) {
                    currentNumber.value = ceil(value).toInt()
                    alpha = value - value.toInt()
                }
                viewModel.setState(State.COLOR_CONFIRMATION)
            }
            Spacer(modifier = Modifier.fillMaxHeight(0.2f))
            RTLText(
                text = "שותף מרוחק מוכן.",
                style = textStyle.copy(fontSize = TEXT_SIZE * 1.5f),
            )
            Spacer(modifier = Modifier.fillMaxHeight(0.1f))
            RTLText(
                text = "הניסוי יתחיל בעוד:",
                style = textStyle.copy(fontSize = TEXT_SIZE * 1.5f),
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
    fun ColorConfirmationScreen(myColor: MainViewModel.PlayerColor, onConfirm: () -> Unit) {
        ButtonedPage(
            buttonText = "הבנתי",
            onClick = onConfirm,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.fillMaxHeight(0.3f))
                val myText = buildAnnotatedString {
                    append("במהלך הניסוי,\n את/ה תהיה השחקן ")
                    when(myColor) {
                        MainViewModel.PlayerColor.BLUE ->
                            withStyle(style = SpanStyle(color = BLUE_COLOR)) {
                                append("הכחול")
                            }
                        MainViewModel.PlayerColor.GREEN ->
                            withStyle(style = SpanStyle(color = GRASS_GREEN_COLOR)) {
                                append("הירוק")
                            }
                    }
                }
                val otherText = buildAnnotatedString {
                    append("והשותף יהיה ")
                    when(myColor) {
                        MainViewModel.PlayerColor.BLUE ->
                            withStyle(style = SpanStyle(color = GRASS_GREEN_COLOR)) {
                                append("ירוק")
                            }
                        MainViewModel.PlayerColor.GREEN ->
                            withStyle(style = SpanStyle(color = BLUE_COLOR)) {
                                append("כחול")
                            }
                    }
                }
                RTLText(
                    text = myText,
                    style = textStyle.copy(fontSize = TEXT_SIZE * 1.2f),
                )
                RTLText(
                    text = otherText,
                    style = textStyle.copy(fontSize = TEXT_SIZE * 1.2f),
                )
            }
        }
    }

    @Composable
    fun AfterGame() {
        val pageCount = remember { 2 }
        val scope = rememberCoroutineScope()
        var firstSliderValue by remember { mutableFloatStateOf(1f) }
        var secondSliderValue by remember { mutableFloatStateOf(1f) }
        val pagerState = rememberPagerState(pageCount = { pageCount })

        ButtonedPage(
            buttonText = "המשך",
            onClick = {
                scope.launch {
                    val nextPage = pagerState.currentPage + 1
                    if(nextPage < pageCount){
                        pagerState.animateScrollToPage(nextPage)
                    } else {
                        viewModel.uploadAnswers(
                            FIRST_QUESTION to firstSliderValue.toString(),
                            SECOND_QUESTION to secondSliderValue.toString()
                        )
                    }
                }
            }
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
                            ) { firstSliderValue = it }
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
                            SliderQuestion(SECOND_QUESTION,secondSliderValue) { secondSliderValue = it }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AfterExperiment(expId: String, userId: String) {
        val pageCount = remember { 3 }
        val scope = rememberCoroutineScope()
        val pagerState = rememberPagerState(pageCount = { pageCount })
        ButtonedPage(
            buttonText = if(pagerState.settledPage == pageCount -1) "סיום" else "המשך",
            onClick = {
                scope.launch {
                    val nextPage = pagerState.currentPage + 1
                    if(nextPage < pageCount){
                        pagerState.animateScrollToPage(nextPage)
                    } else {
                        viewModel.endExperiment()
                    }
                }
            }
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
                            )
                        ){
                            Spacer(Modifier.fillMaxHeight(0.4f))
                            RTLText("נשמח שתענה/י על סקר קצר ע\"י סריקת הברקוד בדף הבא")
                        }
                    }
                    1 -> {
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
                    2 -> {
                        Column(modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = COLUMN_PADDING,
                                end = COLUMN_PADDING
                            )
                        ) {
                            Spacer(Modifier.fillMaxHeight(0.5f))
                            RTLText("תודה על השתתפותך בניסוי ! ")
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SliderQuestion(question: String, sliderValue: Float, modifier: Modifier = Modifier, onValueChanged: (Float) -> Unit) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RTLText(
                modifier = Modifier.fillMaxWidth(),
                text = question
            )
            Slider(
                value = sliderValue,
                onValueChange = onValueChanged,
                valueRange = 1f..7f,
                steps = 5,
            )
            BasicText(
                modifier = Modifier.fillMaxWidth(0.2f),
                text = "${sliderValue.roundToInt()}",
                style = TextStyle(
                    color = Color.White,
                    fontSize = TEXT_SIZE*1.5,
                    textAlign = TextAlign.Center),
            )
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
        textModifier: Modifier = Modifier,
        textStyle: TextStyle = com.imsproject.watch.textStyle,
        content: @Composable () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = DARK_BACKGROUND_COLOR)
                .padding(bottom = (SCREEN_RADIUS * 0.08f).dp)
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
                    .fillMaxWidth(0.55f)
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





