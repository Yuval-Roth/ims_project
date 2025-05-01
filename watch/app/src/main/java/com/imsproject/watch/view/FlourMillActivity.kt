package com.imsproject.watch.view

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.imsproject.common.gameserver.GameType
import com.imsproject.watch.BROWN_COLOR
import com.imsproject.watch.LIGHT_BROWN_COLOR
import com.imsproject.watch.SCREEN_RADIUS
import com.imsproject.watch.utils.cartesianToPolar
import com.imsproject.watch.viewmodel.FlourMillViewModel
import com.imsproject.watch.viewmodel.GameViewModel
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.imsproject.common.utils.Angle
import com.imsproject.watch.R
import com.imsproject.watch.SILVER_COLOR
import com.imsproject.watch.utils.polarToCartesian

class FlourMillActivity : GameActivity(GameType.FLOUR_MILL) {

    private val viewModel : FlourMillViewModel by viewModels<FlourMillViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.onCreate(viewModel)
        setContent {
            Main()
        }
    }

    @Composable
    override fun Main(){
        val state by viewModel.state.collectAsState()
        when(state){
            GameViewModel.State.PLAYING -> FlourMill()
            else -> super.Main()
        }
    }

    @Composable
    fun FlourMill() {
        var flourImageBitmap = remember<ImageBitmap?> { null }
        var rotationAngle by remember { mutableStateOf(Angle(0f)) }
        var lastAngle = remember<Angle?> { null }

        if(flourImageBitmap == null) {
            flourImageBitmap = ImageBitmap.imageResource(id = R.drawable.flour)
        }

        val scaledFlourWidth = remember { (flourImageBitmap.width * 0.2f).toInt() }
        val scaledFlourHeight = remember { (flourImageBitmap.height * 0.2f).toInt() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val pointerEvent = awaitPointerEvent()
                            val inputChange = pointerEvent.changes.first()
                            inputChange.consume()
                            when (pointerEvent.type) {
                                PointerEventType.Press, PointerEventType.Move -> {
                                    val position = inputChange.position
                                    val (_, angle) = cartesianToPolar(position.x, position.y)
                                    val _lastAngle = lastAngle
                                    if(_lastAngle == null) {
                                        lastAngle = angle
                                        continue
                                    }
                                    val direction = if (Angle.isClockwise(_lastAngle,angle)) 1 else -1
                                    val angleDiff = angle - _lastAngle
                                    rotationAngle += (angleDiff / 8) * direction
                                    lastAngle = angle
                                }

                                PointerEventType.Release -> {
                                    lastAngle = null
                                }
                            }
                        }
                    }
                }
            ,
            contentAlignment = Alignment.Center

        ){
            Box(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .background(color = SILVER_COLOR)
            )
            Box( // ground
                modifier = Modifier.Companion
                    .fillMaxSize(0.8f)
                    .clip(shape = CircleShape)
                    .background(color = LIGHT_BROWN_COLOR)
//                    .shadow(
//                        elevation = (SCREEN_RADIUS * 0.5).dp,
//                        CircleShape,
//                        spotColor = Color.Green
//                    )
//                    .shadow(
//                        elevation = (SCREEN_RADIUS * 0.5).dp,
//                        CircleShape,
//                        spotColor = Color.Green
//                    )
            )
            Image(
                painter = painterResource(id = R.drawable.mill),
                contentDescription = null,
                modifier = Modifier
                    .size((SCREEN_RADIUS * 0.7f).dp),
                contentScale = ContentScale.FillBounds
            )
            Image(
                painter = painterResource(id = R.drawable.wheel),
                contentDescription = null,
                modifier = Modifier
                    .size((SCREEN_RADIUS * 0.4f).dp)
                    .graphicsLayer(rotationZ = rotationAngle.floatValue)
                    ,
                contentScale = ContentScale.FillBounds
            )

            val (x,y) = polarToCartesian(SCREEN_RADIUS*0.7f,90.0)
            Canvas(modifier = Modifier.Companion.fillMaxSize()){
                for(i in -50 .. 50 step 10){
                    drawImage(
                        image = flourImageBitmap,
                        dstSize = IntSize(scaledFlourWidth, scaledFlourHeight),
                        dstOffset = IntOffset(x.toInt() - scaledFlourWidth / 2 + i , y.toInt() - scaledFlourHeight / 2),
                    )
                }

                for(i in -40 .. 40 step 10){
                    drawImage(
                        image = flourImageBitmap,
                        dstSize = IntSize(scaledFlourWidth, scaledFlourHeight),
                        dstOffset = IntOffset(x.toInt() - scaledFlourWidth / 2 + i , y.toInt() - scaledFlourHeight / 2 - 5),
                    )
                }
                for(i in -30 .. 30 step 10){
                    drawImage(
                        image = flourImageBitmap,
                        dstSize = IntSize(scaledFlourWidth, scaledFlourHeight),
                        dstOffset = IntOffset(x.toInt() - scaledFlourWidth / 2 + i , y.toInt() - scaledFlourHeight / 2 - 10),
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "FlourMillActivity"
    }
}