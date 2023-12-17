package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.R
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel

@Composable
fun GameScreen(navController: NavController, vm: GameViewModel) {
    val highscore by vm.score.collectAsState()
    val score by vm.score.collectAsState() // Score is its own StateFlow
    val gameState by vm.gameState.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var matchResult by remember { mutableStateOf<Boolean?>(null) }

    val buttonColor = when (matchResult) {
        true -> Color.Green
        false -> Color.Red
        else -> MaterialTheme.colorScheme.primary
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier.padding(32.dp),
                text = "Score = $score",
                style = MaterialTheme.typography.headlineLarge
            )
            // Todo: You'll probably want to change this "BOX" part of the composable
            Spacer(Modifier.height(64.dp))

            val gridSize = 3 // For a 3x3 grid

            if(gameState.gameType == GameType.Visual){
                Column(
                    modifier = Modifier
                        .weight(1f) // This will make the grid take up the rest of the space
                        .fillMaxWidth()
                ) {
                    for (row in 0 until gridSize) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                            /*.weight(1f),*/ // This makes each row take up 1/3 of the vertical space
                        ) {
                            for (col in 0 until gridSize) {
                                val index = row * gridSize + col
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .weight(1f)
                                        .background(
                                            if (index == gameState.eventValue - 1) MaterialTheme.colorScheme.secondary
                                            else MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(32.dp)
                                        ), // Add clickable modifier here
                                    contentAlignment = Alignment.Center
                                ) {

                                }
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { matchResult = vm.checkMatch() },
                    modifier = Modifier.padding(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    content = {
                        Text("MATCH!", style = MaterialTheme.typography.headlineLarge)
                    }
                )
            }
            LaunchedEffect(matchResult) {
                if (matchResult != null) {
                    delay(500)  // Blink duration in milliseconds
                    matchResult = null
                }
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }
}

@Preview
@Composable
fun GameScreenPreview() {
    // Since I am injecting a VM into my homescreen that depends on Application context, the preview doesn't work.
    Surface(){
        GameScreen(navController = rememberNavController(), FakeVM())
    }
}