package com.concordium.wallet.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.concordium.wallet.data.room.WalletDatabase.Companion.VERSION_NUMBER
import com.concordium.wallet.data.room.typeconverter.GlobalTypeConverters

@Database(
    entities = arrayOf(
        Identity::class,
        Account::class,
        Transfer::class,
        Recipient::class,
        EncryptedAmount::class
    ),
    version = VERSION_NUMBER,
    exportSchema = true
)
@TypeConverters(GlobalTypeConverters::class)
abstract class WalletDatabase : RoomDatabase() {

    abstract fun identityDao(): IdentityDao
    abstract fun accountDao(): AccountDao
    abstract fun transferDao(): TransferDao
    abstract fun recipientDao(): RecipientDao
    abstract fun encryptedAmountDao(): EncryptedAmountDao

    companion object {

        const val VERSION_NUMBER = 7

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE transfer_table "
                            + " ADD COLUMN memo TEXT"
                )
            }
        }

        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE account_table "
                            + " ADD COLUMN account_delegation TEXT"
                )
            }
        }

        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE account_table "
                            + " ADD COLUMN account_baker TEXT"
                )
            }
        }

        val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE account_table "
                            + " ADD COLUMN accountIndex INTEGER"
                )
            }
        }

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
                )
                    .addMigrations(MIGRATION_3_4)
                    .addMigrations(MIGRATION_4_5)
                    .addMigrations(MIGRATION_5_6)
                    .addMigrations(MIGRATION_6_7)
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
