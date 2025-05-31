package com.imsproject.watch.view

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.asFloatState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.PickerState
import androidx.wear.compose.material.rememberPickerState
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.COLUMN_PADDING
import com.imsproject.watch.DARK_BACKGROUND_COLOR
import com.imsproject.watch.FIRST_QUESTION
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
import com.imsproject.watch.view.contracts.*
import com.imsproject.watch.viewmodel.MainViewModel
import com.imsproject.watch.viewmodel.MainViewModel.State
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val viewModel : MainViewModel by viewModels<MainViewModel>()
    private lateinit var waterRipples: ActivityResultLauncher<Map<String,Any>>
    private lateinit var wineGlasses: ActivityResultLauncher<Map<String,Any>>
    private lateinit var flourMill: ActivityResultLauncher<Map<String,Any>>
    private lateinit var flowerGarden: ActivityResultLauncher<Map<String,Any>>
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
            Main()
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

    }

    private fun setupSensorsPermission() {
        if (ActivityCompat.checkSelfPermission(applicationContext, applicationContext.getString(R.string.BodySensors)) == PackageManager.PERMISSION_DENIED){
            requestPermissions(arrayOf(Manifest.permission.BODY_SENSORS), 0)
        }
    }

    @Composable
    private fun Main(){

        val state = viewModel.state.collectAsState().value

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
                ConnectedInLobbyScreen(userId, lobbyId,gameType,gameDuration, ready, hrSensorReady){
                    viewModel.toggleReady()
                }
            }

            State.IN_GAME -> {
                BlankScreen()
                val input = mutableMapOf<String,Any>(
                    "timeServerStartTime" to viewModel.gameStartTime.collectAsState().value,
                    "additionalData" to viewModel.additionalData.collectAsState().value,
                    "syncTolerance" to (viewModel.syncTolerance.collectAsState().value ?: -1L),
                    "syncWindowLength" to (viewModel.syncWindowLength .collectAsState().value ?: -1L)
                )
                when(viewModel.gameType.collectAsState().value) {
                    GameType.WATER_RIPPLES -> waterRipples.launch(input)
                    GameType.WINE_GLASSES -> wineGlasses.launch(input)
                    GameType.FLOUR_MILL -> flourMill.launch(input)
                    GameType.FLOWER_GARDEN -> flowerGarden.launch(input)
                    else -> {
                        viewModel.showError("Unknown game type")
                        ErrorReporter.report(null,"Unknown game type\n${viewModel.gameType.collectAsState().value}")
                    }
                }
            }

            State.UPLOADING_EVENTS -> LoadingScreen("מעלה אירועים....")

            State.AFTER_GAME -> AfterGame()

            State.UPLOADING_ANSWERS -> LoadingScreen("מעלה תשובות....")

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
    }

    @Composable
    fun AlreadyConnectedScreen(onConfirm: () -> Unit, onReject: () -> Unit) {
        MaterialTheme {
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
                    RTLText(
                        text = "המזהה שבחרת כבר מחובר ממקום אחר.\n\nהאם תרצה/י להתחבר בכל זאת?",
                        style = textStyle,
                    )
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
    }

    @Composable
    fun BlankScreen() {
        MaterialTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DARK_BACKGROUND_COLOR),
                contentAlignment = Alignment.Center
            ){

            }
        }
    }

    @Composable
    private fun LoadingScreen(text: String) {
        MaterialTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DARK_BACKGROUND_COLOR),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        strokeWidth = (SCREEN_RADIUS * 0.04f).dp,
                        modifier = Modifier.fillMaxSize(0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    RTLText(
                        text = text,
                        style = textStyle
                    )
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
        MaterialTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DARK_BACKGROUND_COLOR),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ){
                    Column(
                        modifier = Modifier
                            .padding(
                                top = COLUMN_PADDING,
                                start = COLUMN_PADDING,
                                end = COLUMN_PADDING
                            )
                            .fillMaxWidth()
                            .fillMaxHeight(0.5f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ){
                        RTLText(
                            text = "המזהה שלי: $id",
                            style = textStyle,
                        )
                        Spacer(modifier = Modifier.height((SCREEN_RADIUS*0.1f).dp))
                        RTLText(
                            text = "מחכה לצירוף ללובי...",
                            style = textStyle,
                        )
                    }
                    Spacer(modifier = Modifier.height((SCREEN_RADIUS*0.2f).dp))
                    // return to picking id button
                    Button(
                        onClick = { viewModel.exit() },
                        modifier = Modifier
                            .fillMaxSize(),
                        shape = RectangleShape,
                    ) {
                        RTLText(
                            text = "החלף מזהה",
                            style = textStyle.copy(color = Color.Black),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ConnectedInLobbyScreen(
        userId: String,
        lobbyId: String,
        gameType: String,
        gameDuration: String,
        ready: Boolean,
        hrSensorReady: Boolean,
        onReady: () -> Unit
    ) {
        MaterialTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DARK_BACKGROUND_COLOR),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ){
                    Spacer(modifier = Modifier.height((SCREEN_RADIUS*0.05f).dp))
                    val white = remember { Color(0xFFFFE095) }
                    val green = remember { Color(0xFF89F55C) }
                    Box(
                        modifier = Modifier
                            .background(if (ready) green else white)
                            .fillMaxWidth()
                            .fillMaxHeight(0.15f)
                        ,
                        contentAlignment = Alignment.Center,
                    ){
                        RTLText(
                            text = "סטטוס: "+if (ready) "מוכן" else "לא מוכן",
                            style = textStyle.copy(color = Color.Black),
                        )
                    }
                    Column(
                        modifier = Modifier
                            .padding(
                                start = COLUMN_PADDING - 10.dp,
                                end = COLUMN_PADDING - 10.dp,
                                bottom = COLUMN_PADDING - 10.dp
                            )
                            .fillMaxSize()
                        ,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ){
                        val textStyle = TextStyle(
                            color = Color(0xFF707070),
                            fontSize = TEXT_SIZE
                        )
                        RTLText(
                            text = "המזהה שלי: $userId",
                            style = textStyle,
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        RTLText(
                            text = "מזהה לובי: $lobbyId",
                            style = textStyle,
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        RTLText(
                            text = gameType.ifBlank { "" },
                            style = textStyle,
                            Modifier.height((SCREEN_RADIUS * 0.07f).dp)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        RTLText(
                            text = if(gameDuration.isNotBlank()) "$gameDuration שניות" else "",
                            style = textStyle,
                            Modifier.height((SCREEN_RADIUS * 0.07f).dp)
                        )
                        Spacer(modifier = Modifier.fillMaxHeight(0.25f))
                        // Button
                        Button(
                            colors = if(hrSensorReady) ButtonDefaults.primaryButtonColors()
                            else ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF707070).copy(alpha=0.5f),
                                contentColor = Color.White
                            ),
                            onClick = { onReady() },
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .fillMaxHeight(0.65f)

                        ) {
                            BasicText(
                                text = if(ready) "השהה התחלה" else "אפשר להתחיל",
                                style = textStyle.copy(color=Color.Black, letterSpacing = 1.25.sp),
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AfterGame() {
        var firstSliderValue by remember { mutableFloatStateOf(1f) }
        var secondSliderValue by remember { mutableFloatStateOf(1f) }
        val pagerState = rememberPagerState(pageCount = {3})

        MaterialTheme {
            Box(
                modifier = Modifier
                    .background(color = DARK_BACKGROUND_COLOR)
                    .fillMaxSize()
            ){
                VerticalPager(
                    state = pagerState,
                ){ page ->
                    when(page) {
                        0 -> {
                            Box(modifier = Modifier.fillMaxWidth()){
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(
                                            top = COLUMN_PADDING * 2f,
                                            start = 20.dp,
                                            end = 20.dp
                                        )
                                    , horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    SliderQuestion(FIRST_QUESTION,firstSliderValue) { firstSliderValue = it }
                                    Spacer(Modifier.height((SCREEN_RADIUS * 0.1f).dp))
                                    ScrollHintArrow(! pagerState.isScrollInProgress)
                                }
                            }
                        }
                        1 -> {
                            Box(modifier = Modifier.fillMaxSize()){
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(
                                            top = COLUMN_PADDING / 4f,
                                            start = 20.dp,
                                            end = 20.dp
                                        )
                                    , horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    ScrollHintArrow(!pagerState.isScrollInProgress, backwards = true)
                                    Spacer(Modifier.height((SCREEN_RADIUS * 0.1f).dp))
                                    SliderQuestion(SECOND_QUESTION,secondSliderValue) { secondSliderValue = it }
                                    Spacer(Modifier.height((SCREEN_RADIUS * 0.1f).dp))
                                    ScrollHintArrow(!pagerState.isScrollInProgress)
                                }
                            }
                        }
                        2 -> {
                            val pageOffset = remember { derivedStateOf { pagerState.currentPageOffsetFraction } }
                            if(pagerState.currentPage == 2 && pageOffset.value > -0.05f){
                                viewModel.uploadAnswers(
                                    FIRST_QUESTION to firstSliderValue.toString(),
                                    SECOND_QUESTION to secondSliderValue.toString()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AfterExperiment(expId: String, userId: String) {
        val pagerState = rememberPagerState(pageCount = {4})

        MaterialTheme {
            Box(
                modifier = Modifier
                    .background(color = DARK_BACKGROUND_COLOR)
                    .fillMaxSize()
            ){
                VerticalPager(
                    state = pagerState,
                ){ page ->
                    when(page) {
                        0 -> {
                            Box(modifier = Modifier.fillMaxWidth()){
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(
                                            top = COLUMN_PADDING * 2f,
                                            start = 20.dp,
                                            end = 20.dp
                                        )
                                    , horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Spacer(Modifier.height((SCREEN_RADIUS * 0.15f).dp))
                                    RTLText(
                                        text = "נשמח שתענה/י על סקר קצר ע\"י סריקת הברקוד בדף הבא",
                                        style = textStyle
                                    )
                                    Spacer(Modifier.height((SCREEN_RADIUS * 0.25f).dp))
                                    ScrollHintArrow(! pagerState.isScrollInProgress)
                                }
                            }
                        }
                        1 -> {
                            Box(modifier = Modifier.fillMaxSize()){
                                Column(
                                    modifier = Modifier.fillMaxSize()
                                    , horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    ScrollHintArrow(!pagerState.isScrollInProgress, backwards = true)
                                    ExperimentQuestionsQRCode(expId,userId,Modifier.size((SCREEN_RADIUS*0.7f).dp))
                                    ScrollHintArrow(!pagerState.isScrollInProgress)
                                }
                            }
                        }
                        2 -> {
                            Box(modifier = Modifier.fillMaxSize()){
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    ScrollHintArrow(!pagerState.isScrollInProgress, backwards = true)
                                    RTLText(
                                        text = "תודה על השתתפותך בניסוי ! \n החלק למטה כדי להמשיך.",
                                        style = textStyle
                                    )
                                    ScrollHintArrow(!pagerState.isScrollInProgress)
                                }
                            }
                        }
                        3 -> {
                            val pageOffset = remember { derivedStateOf { pagerState.currentPageOffsetFraction } }
                            if(pagerState.currentPage == 3 && pageOffset.value > -0.05f){
                                viewModel.endExperiment()
                            }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SliderQuestion(question: String, sliderValue: Float, onValueChanged: (Float) -> Unit) {
        RTLText(
            modifier = Modifier.fillMaxWidth(),
            text = question,
            style = textStyle,
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


    @Preview(device = "id:wearos_large_round")
    @Composable
    private fun PreviewConnectedInLobbyScreen() {
        initProperties(454)
        MaterialTheme {
            ConnectedInLobbyScreen(
                userId = "001",
                lobbyId = "0001",
                gameType = "אדוות מים",
                gameDuration = "60",
                ready = true,
                hrSensorReady = true,
            ) { }
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
}





