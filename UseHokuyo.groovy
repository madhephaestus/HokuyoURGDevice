import com.neuronrobotics.bowlerstudio.BowlerStudioController
import com.neuronrobotics.bowlerstudio.physics.TransformFactory
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine
import com.neuronrobotics.sdk.addons.kinematics.math.TransformNR
import com.neuronrobotics.sdk.common.DeviceManager

import eu.mihosoft.vrl.v3d.CSG
import eu.mihosoft.vrl.v3d.Cube
import javafx.application.Platform
import javafx.scene.transform.Affine

ScriptingEngine.gitScriptRun("https://github.com/madhephaestus/HokuyoURGDevice.git", "HokuyoURGDevice.groovy")

def lidar  =DeviceManager.getSpecificDevice("lidar")

ArrayList<CSG> displayBits = null
try {
	while(lidar.isConnected()&& !Thread.interrupted()) {
		def sweep = lidar.startSweep(-45, 45, 5)
		def data=sweep.getData()

		if(displayBits==null) {
			displayBits=[]
			for(def bit:data) {
				//TransformNR pose = bit.getPosition();
				CSG dot = new Cube(5).toCSG()
				dot.setManipulator(new Affine())
				BowlerStudioController.addCsg(dot)
				displayBits.add(dot)
			}
		}
		Platform.runLater({
			for(int i=0;i<data.size();i++) {
				def bit= data.get(i)
				TransformFactory.nrToAffine(bit.getPosition(),displayBits.get(i).getManipulator())
			}
		})
	}
}catch(Throwable t) {
	t.printStackTrace()
}
for(CSG dot:displayBits) {
	BowlerStudioController.removeObject(dot)
}