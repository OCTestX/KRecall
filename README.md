# KRecall

# How to compile
## Use conveyor to compile the project.

### Output Test Dir

`./gradlew desktopJar`
For Windows:
`conveyor make windows-app`
For current OS:
`conveyor make app`
More information see: https://conveyor.hydraulic.dev/17.0/tutorial/hare/jvm/#__tabbed_2_2


### Output web
`./gradlew desktopJar`

`conveyor make site`

# Use

## About Capture audio

在捕获音频时只写入WAV 头

距离wav完整文件还需要

``` kotlin
// 更新 WAV 头中的长度信息
WavHeaderUtil.updateHeader(file)
```

## About STT
https://alphacephei.com/vosk/models