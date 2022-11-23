package com.concordium.wallet.data.room

import android.content.Context
import androidx.room.*
import com.concordium.wallet.data.room.WalletDatabase.Companion.VERSION_NUMBER
import com.concordium.wallet.data.room.typeconverter.GlobalTypeConverters

@Database(
    entities = [Identity::class, Account::class, Transfer::class, Recipient::class, EncryptedAmount::class, AccountContract::class, ContractToken::class],
    version = VERSION_NUMBER,
    exportSchema = true,
    autoMigrations = [
        AutoMigration (from = 7, to = 8),
        AutoMigration (from = 8, to = 9)
    ]
)
@TypeConverters(GlobalTypeConverters::class)
abstract class WalletDatabase : RoomDatabase() {

    abstract fun identityDao(): IdentityDao
    abstract fun accountDao(): AccountDao
    abstract fun transferDao(): TransferDao
    abstract fun recipientDao(): RecipientDao
    abstract fun encryptedAmountDao(): EncryptedAmountDao
    abstract fun accountContractDao(): AccountContractDao
    abstract fun contractTokenDao(): ContractTokenDao

    companion object {
        const val VERSION_NUMBER = 9

        // Singleton prevents multiple instances of database opening at the same time.
        @Volatile
        private var INSTANCE: WalletDatabase? = null

        fun getDatabase(context: Context): WalletDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WalletDatabase::class.java,
                    "wallet_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
