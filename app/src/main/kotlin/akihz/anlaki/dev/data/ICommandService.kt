package akihz.anlaki.dev.data

import android.os.Binder
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel

interface ICommandService : IInterface {
    /**
     * Runs a shell command.
     * @param command shell line to run.
     * @return output text, or an ERROR message.
     */
    fun runCommand(command: String): String
    /**
     * Runs a settings command.
     * @param arguments operation, namespace, key, and optional value.
     * @return output text, or an ERROR message.
     */
    fun runSettingsCommand(arguments: List<String>): String
    /** Stops the user service process. */
    fun destroy()
    /** Returns the user service process id. */
    fun getPid(): Int

    abstract class Stub : Binder(), ICommandService {
        init {
            this.attachInterface(this, DESCRIPTOR)
        }

        /** Returns this binder. */
        override fun asBinder(): IBinder = this

        /**
         * Dispatches one binder transaction to its handler.
         * @return true when the code was handled.
         */
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            val descriptor = DESCRIPTOR
            if (code in IBinder.FIRST_CALL_TRANSACTION..0x00FFFFFF) {
                data.enforceInterface(descriptor)
            }
            if (code == IBinder.INTERFACE_TRANSACTION) {
                reply?.writeString(descriptor)
                return true
            }
            return when (code) {
                TRANSACTION_runCommand -> {
                    val arg0 = data.readString() ?: ""
                    val result = this.runCommand(arg0)
                    reply?.writeNoException()
                    reply?.writeString(result)
                    true
                }
                TRANSACTION_runSettingsCommand -> {
                    val arguments = data.createStringArrayList() ?: arrayListOf()
                    val result = this.runSettingsCommand(arguments)
                    reply?.writeNoException()
                    reply?.writeString(result)
                    true
                }
                TRANSACTION_destroy -> {
                    this.destroy()
                    reply?.writeNoException()
                    true
                }
                TRANSACTION_getPid -> {
                    val result = this.getPid()
                    reply?.writeNoException()
                    reply?.writeInt(result)
                    true
                }
                else -> super.onTransact(code, data, reply, flags)
            }
        }

        companion object {
            const val DESCRIPTOR = "akihz.anlaki.dev.data.ICommandService"
            const val TRANSACTION_runCommand = IBinder.FIRST_CALL_TRANSACTION + 0
            const val TRANSACTION_runSettingsCommand = IBinder.FIRST_CALL_TRANSACTION + 1
            const val TRANSACTION_destroy = IBinder.FIRST_CALL_TRANSACTION + 2
            const val TRANSACTION_getPid = IBinder.FIRST_CALL_TRANSACTION + 3

            /**
             * Wraps a binder as [ICommandService].
             * @return local interface, or a remote proxy.
             */
            @JvmStatic
            fun asInterface(obj: IBinder?): ICommandService? {
                if (obj == null) return null
                val iin = obj.queryLocalInterface(DESCRIPTOR)
                if (iin != null && iin is ICommandService) {
                    return iin
                }
                return Proxy(obj)
            }
        }

        private class Proxy(private val mRemote: IBinder) : ICommandService {
            /** Returns the remote binder. */
            override fun asBinder(): IBinder = mRemote

            /** Runs a shell command on the remote service. */
            override fun runCommand(command: String): String {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                val result: String
                try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeString(command)
                    mRemote.transact(TRANSACTION_runCommand, data, reply, 0)
                    reply.readException()
                    result = reply.readString() ?: ""
                } finally {
                    reply.recycle()
                    data.recycle()
                }
                return result
            }

            /** Runs a settings command on the remote service. */
            override fun runSettingsCommand(arguments: List<String>): String {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    data.writeStringList(arguments)
                    mRemote.transact(TRANSACTION_runSettingsCommand, data, reply, 0)
                    reply.readException()
                    reply.readString() ?: ""
                } finally {
                    reply.recycle()
                    data.recycle()
                }
            }

            /** Asks the remote service to stop. */
            override fun destroy() {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    mRemote.transact(TRANSACTION_destroy, data, reply, 0)
                    reply.readException()
                } finally {
                    reply.recycle()
                    data.recycle()
                }
            }

            /** Returns the remote service process id. */
            override fun getPid(): Int {
                val data = Parcel.obtain()
                val reply = Parcel.obtain()
                return try {
                    data.writeInterfaceToken(DESCRIPTOR)
                    mRemote.transact(TRANSACTION_getPid, data, reply, 0)
                    reply.readException()
                    reply.readInt()
                } finally {
                    reply.recycle()
                    data.recycle()
                }
            }
        }
    }
}
