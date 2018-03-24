package info.vividcode.wdip.jmx

import java.lang.management.ManagementFactory
import javax.management.remote.JMXServiceURL
import javax.management.ObjectName
import javax.management.remote.JMXConnectorFactory
import javax.management.JMX

// https://docs.oracle.com/javase/jp/1.5.0/guide/jmx/tutorial/essential.html

interface TerminatorMBean {
    fun shutdown()
}

class Terminator : TerminatorMBean {
    override fun shutdown() {
        Thread(Runnable {
            try { Thread.sleep(50) } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            Runtime.getRuntime().exit(0)
        }).start()
    }
}

object TerminatorHandler {

    private val name = ObjectName("info.vividcode.wdip.jmx:type=Terminator")

    fun registerTerminator() {
        val mbs = ManagementFactory.getPlatformMBeanServer()
        val terminator = Terminator()
        mbs.registerMBean(terminator, name)
    }

    fun callRemoteShutdown(portNumber: Int) {
        val url = JMXServiceURL("service:jmx:rmi:///jndi/rmi://:$portNumber/jmxrmi")
        JMXConnectorFactory.connect(url, emptyMap<String, Any>()).use { connector ->
            val terminator = JMX.newMBeanProxy(connector.mBeanServerConnection, name, TerminatorMBean::class.java)
            terminator.shutdown()
        }
    }

}
