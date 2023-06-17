import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine
import com.neuronrobotics.sdk.common.DeviceManager

ScriptingEngine.gitScriptRun("https://github.com/madhephaestus/HokuyoURGDevice.git", "HokuyoURGDevice.groovy")

def lidar  =DeviceManager.getSpecificDevice("lidar")

def sweep = lidar.startSweep(-90, 90, 0.5) 