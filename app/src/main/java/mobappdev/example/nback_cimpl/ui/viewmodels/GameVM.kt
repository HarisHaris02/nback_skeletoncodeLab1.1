package mobappdev.example.nback_cimpl.ui.viewmodels

import TTSManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.GameApplication
import mobappdev.example.nback_cimpl.NBackHelper
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository

/**
 * This is the GameViewModel.
 *
 * It is good practice to first make an interface, which acts as the blueprint
 * for your implementation. With this interface we can create fake versions
 * of the viewmodel, which we can use to test other parts of our app that depend on the VM.
 *
 * Our viewmodel itself has functions to start a game, to specify a gametype,
 * and to check if we are having a match
 *
 * Date: 25-08-2023
 * Version: Version 1.0
 * Author: Yeetivity
 *
 */


interface GameViewModel {
    val gameState: StateFlow<GameState>
    val score: StateFlow<Int>
    val highscore: StateFlow<Int>
    val nBack: Int
    val isGameFinished: StateFlow<Int>

    fun setGameType(gameType: GameType)
    fun startGame()
    fun checkMatch(): Boolean
    fun cancelJob()
}


class GameVM(
    private val userPreferencesRepository: UserPreferencesRepository,
    context: Context
): GameViewModel, ViewModel() {
    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState>
        get() = _gameState.asStateFlow()

    private val _score = MutableStateFlow(0)
    override val score: StateFlow<Int>
        get() = _score

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int>
        get() = _highscore

    // nBack is currently hardcoded
    override val nBack: Int = 2

    private val _isGameFinished = MutableStateFlow(0)
    override val isGameFinished: StateFlow<Int>
        get() = _isGameFinished

    private var job: Job? = null  // coroutine job for the game event
    private val eventInterval: Long = 2000L  // 2000 ms (2s)

    private val nBackHelper = NBackHelper()  // Helper that generate the event array
    private var events = emptyArray<Int>()  // Array with all events

    private var matchCheckedForCurrentEvent = false
    private var currentIndex = 0

    private val ttsManager = TTSManager(context)

    override fun setGameType(gameType: GameType) {
        // update the gametype in the gamestate
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    override fun startGame() {
        job?.cancel()  // Cancel any existing game loop

        // Get the events from our C-model (returns IntArray, so we need to convert to Array<Int>)
        val nrOfEvents = 20
        events = nBackHelper.generateNBackString(nrOfEvents, 9, 30, nBack).toList().toTypedArray()  // Todo Higher Grade: currently the size etc. are hardcoded, make these based on user input
        //events = arrayOf(1, 2, 9, 2, 1, 3, 2, 7, 3, 7)

        Log.d("GameVM", "The following sequence was generated: ${events.contentToString()}")

        _isGameFinished.value = 0
        _score.value = 0

        job = viewModelScope.launch {
            when (gameState.value.gameType) {
                GameType.Audio -> runAudioGame()
                GameType.AudioVisual -> runAudioVisualGame()
                GameType.Visual -> runVisualGame(events)
            }
            _isGameFinished.value = 1
            if (_score.value > _highscore.value) {
                _highscore.value = _score.value
                userPreferencesRepository.saveHighScore(_highscore.value)
            }
        }
    }
    override fun checkMatch(): Boolean {
        if (!matchCheckedForCurrentEvent && currentIndex >= nBack) {
            val isMatch = events[currentIndex - nBack] == gameState.value.eventValue
            if (isMatch) {
                _score.value += 1
            }
            matchCheckedForCurrentEvent = true
            return isMatch
        }
        return false
    }

    override fun cancelJob() {
        job?.cancel()
        ttsManager.stop()
    }
    private fun runAudioGame() {
       job = viewModelScope.launch {
           _isGameFinished.value = 0
            events.forEachIndexed { index, eventNumber ->
                if(!isActive){
                    ttsManager.stop()
                    return@forEachIndexed
                }
                val letter = numberToLetter(eventNumber)
                ttsManager.speak(letter)

                currentIndex = index
                _gameState.value = _gameState.value.copy(eventValue = eventNumber)

                delay(eventInterval)
                ttsManager.stop()
                matchCheckedForCurrentEvent = false
            }
        }
    }


    private fun numberToLetter(number: Int): String {
        return when (number-1) {
            0 -> "A"
            1 -> "B"
            2 -> "C"
            3 -> "D"
            4 -> "E"
            5 -> "F"
            6 -> "G"
            7 -> "H"
            8 -> "I"
            else -> "Invalid"
        }
    }

    override fun onCleared() {
        ttsManager.shutdown()
        super.onCleared()
    }

    private suspend fun runVisualGame(events: Array<Int>){
        // Todo: Replace this code for actual game code
        for ((index, value) in events.withIndex()) {
            currentIndex = index // Update the current index
            _gameState.value = _gameState.value.copy(eventValue = value)
            matchCheckedForCurrentEvent = false
            delay(eventInterval)
        }
    }

    private fun runAudioVisualGame(){
        // Todo: Make work for Higher grade
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as GameApplication)
                GameVM(application.userPreferencesRespository, application.applicationContext)
            }
        }
    }


    init {
        // Code that runs during creation of the vm
        viewModelScope.launch {
            userPreferencesRepository.highscore.collect {
                _highscore.value = it
            }
        }
    }
}

// Class with the different game types
enum class GameType{
    Audio,
    Visual,
    AudioVisual
}

data class GameState(
    // You can use this state to push values from the VM to your UI.
    val gameType: GameType = GameType.Visual,  // Type of the game
    val eventValue: Int = -1  // The value of the array string
)

class FakeVM: GameViewModel{
    override val gameState: StateFlow<GameState>
        get() = MutableStateFlow(GameState()).asStateFlow()
    override val score: StateFlow<Int>
        get() = MutableStateFlow(5).asStateFlow()
    override val highscore: StateFlow<Int>
        get() = MutableStateFlow(0).asStateFlow()
    override val nBack: Int
        get() = 2
    override val isGameFinished: StateFlow<Int>
        get() = MutableStateFlow(0).asStateFlow()

    override fun setGameType(gameType: GameType) {
    }

    override fun startGame() {
    }

    override fun checkMatch(): Boolean {
        return true
    }

    override fun cancelJob() {
        TODO("Not yet implemented")
    }
}