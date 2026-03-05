package akihz.anlaki.dev

import android.os.Binder
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel

interface ICommandService : IInterface {
    fun runCommand(command: String): String
    fun destroy()

    abstract class Stub : Binder(), ICommandService {
        init {
            this.attachInterface(this, DESCRIPTOR)
        }

        override fun asBinder(): IBinder = this

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
                TRANSACTION_destroy -> {
                    this.destroy()
                    reply?.writeNoException()
                    true
                }
                else -> super.onTransact(code, data, reply, flags)
            }
        }

        companion object {
            const val DESCRIPTOR = "akihz.anlaki.dev.ICommandService"
            const val TRANSACTION_runCommand = IBinder.FIRST_CALL_TRANSACTION + 0
            const val TRANSACTION_destroy = IBinder.FIRST_CALL_TRANSACTION + 1

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
            override fun asBinder(): IBinder = mRemote

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
        }
    }
}