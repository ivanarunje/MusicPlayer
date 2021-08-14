# MusicPlayer

Music player is an application that plays audio files in MP3 format.

It consists of 2 main application screens, first one is library where all audio files are listen and user can select any song from the list.
Song selection opens second screen in which user can use audio controls like play, stop, forward, backward, previous, next. All of listed controls are also available via voice
recognition by holding microphone button. User can also go back to library by clicking Library icon on bottom left corner of navigation menu.

Application uses WaveRecorder API for recording audio in WAVE format with the corresponding header and Tensorflow Lite API to open tflite model and run inference on recorded audio file.

For opening audio files and generating MFCC features jLibrosa library is used which is Java implementation of librosa python API that is used for training the model.
