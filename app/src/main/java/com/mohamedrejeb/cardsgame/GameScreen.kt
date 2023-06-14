package com.mohamedrejeb.cardsgame

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mohamedrejeb.cardsgame.ui.theme.CardsGameTheme
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@Composable
fun GameScreen() {
    val context = LocalContext.current
    val density = LocalDensity.current

    val cards = remember {
        mutableStateOf(Data.cardList)
    }
    val cardsSpreadDegree = remember {
        mutableStateOf(10f)
    }
    val activeCard = remember {
        mutableStateOf<Card?>(null)
    }
    val droppedCards = remember {
        mutableListOf<Card>()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = Color(0xFF266B35)
            )
    ) {

        Data.cardList.indices.forEach { index ->
            CardItem(
                card = Card(id = index, imageRes = R.drawable.card_back),
                index = index,
                nonDroppedCardsSize = cards.value.size,
                transformOrigin = TransformOrigin(1f, 0f),
                enableDrag = false,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = (-60).dp, y = 100.dp)
            )
        }

        cards.value.forEachIndexed { index, card ->
            key(card.id) {
                CardItem(
                    card = card,
                    index = index,
                    transformOrigin = TransformOrigin(0f, 1f),
                    nonDroppedCardsSize = cards.value.size - droppedCards.size,
                    activeCard = activeCard.value,
                    cardsSpreadDegree = cardsSpreadDegree.value,
                    isDropped = droppedCards.contains(card),
                    onCardDropped = { droppedCard ->
                        droppedCards.add(droppedCard)
                    },
                    setActiveCard = { activeCard.value = it },
                    getTargetOffset = {
                        val width = 50f * droppedCards.size
                        Offset(
                            x = width,
                            y = with(density) { maxHeight.toPx() / 2 } - 450f,
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(x = 60.dp, y = (-100).dp)
                        .then(
                            if (droppedCards.contains(card)) {
                                Modifier
                                    .zIndex(
                                        droppedCards
                                            .indexOf(card)
                                            .toFloat()
                                    )
                            } else {
                                Modifier
                                    .zIndex((droppedCards.size + index).toFloat())
                            }
                        )
                )
            }
        }

        PlayerHand(
            cardsSpreadDegree = cardsSpreadDegree,
            onHandDragged = { delta ->
                val newCardsSpreadDegree = max(
                    0f,
                    min(
                        12f,
                        cardsSpreadDegree.value + delta / 10f
                    )
                )
                cardsSpreadDegree.value = newCardsSpreadDegree
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = 20.dp, y = (-10).dp)
                .zIndex((droppedCards.size + cards.value.size).toFloat())
        )

    }

}

@Composable
fun CardItem(
    card: Card,
    index: Int,
    transformOrigin: TransformOrigin,
    modifier: Modifier = Modifier,
    nonDroppedCardsSize: Int = 0,
    isDropped: Boolean = false,
    onCardDropped: (Card) -> Unit = {},
    getTargetOffset: () -> Offset = { Offset.Zero },
    enableDrag: Boolean = true,
    activeCard: Card? = null,
    setActiveCard: (Card?) -> Unit = {},
    cardsSpreadDegree: Float = 10f,
) {
    val scope = rememberCoroutineScope()
    val isBeingDragged = remember {
        mutableStateOf(false)
    }
    val activeCardOffset = animateFloatAsState(
        targetValue = if (activeCard == card && !isBeingDragged.value) -100f else 0f,
        label = "Active card ${card.id} offset animation"
    )
    val cardRotation = animateFloatAsState(
        targetValue = if (isDropped) 0f else cardsSpreadDegree * (index - nonDroppedCardsSize / 2) - 30f,
        label = "Card ${card.id} rotation animation"
    )
    val cardDropRotation = animateFloatAsState(
        targetValue = if (isDropped) 160f else 0f,
        label = "Card ${card.id} drop rotation animation",
        animationSpec = tween(
            durationMillis = 400,
            easing = EaseInOut,
        )
    )
    val cardDragX = remember {
        Animatable(initialValue = 0f,)
    }
    val cardDragY = remember {
        Animatable(initialValue = 0f,)
    }
    val cardOriginalOffset = remember {
        mutableStateOf(Offset.Zero)
    }

    Image(
        painter = painterResource(id = card.imageRes),
        contentDescription = "Card ${card.id}",
        modifier = modifier
            .width(120.dp)
            .wrapContentHeight()
            .onGloballyPositioned {
                cardOriginalOffset.value = it.positionInRoot() - Offset(
                    x = it.size.width / 2f,
                    y = it.size.height / 2f,
                )
            }
            .graphicsLayer {
                this.transformOrigin = transformOrigin
                rotationZ = cardRotation.value
            }
            .graphicsLayer {
                translationX = cardDragX.value
                translationY = activeCardOffset.value + cardDragY.value
            }
            .graphicsLayer {
                this.transformOrigin = TransformOrigin.Center
                rotationZ = cardDropRotation.value
            }
            .clip(MaterialTheme.shapes.small)
            .pointerInput(activeCard) {
                detectTapGestures(
                    onTap = {
                        if (isDropped) return@detectTapGestures
                        setActiveCard(
                            if (activeCard == card) null else card
                        )
                    },
                )
            }
            .then(
                if (enableDrag && !isDropped) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                println("startOffset: $startOffset")
                                isBeingDragged.value = true
                                setActiveCard(card)
                            },
                            onDragEnd = {
                                isBeingDragged.value = false

                                val dragOffset = Offset(
                                    x = cardDragX.value,
                                    y = cardDragY.value,
                                )
                                val distance = calculateDistanceBetweenTwoPoints(
                                    dragOffset,
                                    Offset.Zero
                                )
                                val targetOffset = getTargetOffset()

                                println("targetOffset: $targetOffset")
                                println("originalOffset: ${cardOriginalOffset.value}")
                                println("drag offset: ${cardDragX.value}, ${cardDragY.value}")
                                println("Distance: $distance")

                                if (distance > 500) {
                                    val remainingOffset = targetOffset - cardOriginalOffset.value
                                    println("remainingOffset: $remainingOffset")
                                    scope.launch {
                                        cardDragX.animateTo(
                                            targetValue = remainingOffset.x,
                                            animationSpec = tween(
                                                durationMillis = 800,
                                                easing = EaseInOut
                                            )
                                        )
                                    }
                                    scope.launch {
                                        cardDragY.animateTo(
                                            targetValue = remainingOffset.y,
                                            animationSpec = tween(
                                                durationMillis = 800,
                                                easing = EaseInOut
                                            )
                                        )
                                    }
                                    onCardDropped(card)
                                } else {
                                    scope.launch {
                                        cardDragX.animateTo(0f)
                                    }
                                    scope.launch {
                                        cardDragY.animateTo(0f)
                                    }
                                }

                                setActiveCard(null)
                            },
                            onDragCancel = {
                                isBeingDragged.value = false
                                scope.launch {
                                    cardDragX.animateTo(0f)
                                }
                                scope.launch {
                                    cardDragY.animateTo(0f)
                                }
                                setActiveCard(null)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()

                                scope.launch {
                                    cardDragX.snapTo(cardDragX.value + dragAmount.x)
                                }
                                scope.launch {
                                    cardDragY.snapTo(cardDragY.value + dragAmount.y)
                                }
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
            .shadow(
                elevation = 10.dp,
                shape = MaterialTheme.shapes.small,
            )
            .then(
                if (activeCard == card) {
                    Modifier
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small
                        )
                } else {
                    Modifier
                }
            )
    )
}

@Composable
fun PlayerHand(
    cardsSpreadDegree: State<Float>,
    onHandDragged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val isHandBeingDragged = remember {
        mutableStateOf(false)
    }

    Image(
        painter = painterResource(id = R.drawable.hand),
        contentDescription = "hand",
        contentScale = ContentScale.FillWidth,
        modifier = modifier
            .width(100.dp)
            .wrapContentHeight()
            .graphicsLayer {
                transformOrigin = TransformOrigin(0f, 0f)
                rotationZ = cardsSpreadDegree.value - 10f
            }
            .draggable(
                state = rememberDraggableState { delta ->
                    onHandDragged(delta)
                },
                orientation = Orientation.Horizontal,
                onDragStarted = {
                    isHandBeingDragged.value = true
                },
                onDragStopped = {
                    isHandBeingDragged.value = false
                }
            )
    )
}

@Preview(showBackground = true)
@Composable
private fun GameScreenPreview() {
    CardsGameTheme {
        GameScreen()
    }
}