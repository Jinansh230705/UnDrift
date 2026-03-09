package com.undrift.data

import android.util.Log
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.ServerApi
import com.mongodb.ServerApiVersion
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.bson.Document
import java.util.concurrent.TimeUnit

class MongoRepository {
    private val TAG = "MongoRepository"
    
    init {
        try {
            System.setProperty("com.mongodb.disableJndi", "true")
            Log.d(TAG, "JNDI disabled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set disableJndi property: ${e.message}")
        }
    }

    private val connectionStringValue = "mongodb://main:undrift_starter@" +
            "ac-1zcpaet-shard-00-00.ey4ivza.mongodb.net:27017," +
            "ac-1zcpaet-shard-00-01.ey4ivza.mongodb.net:27017," +
            "ac-1zcpaet-shard-00-02.ey4ivza.mongodb.net:27017/" +
            "?tls=true&replicaSet=atlas-gaunjv-shard-0&authSource=admin&retryWrites=true&w=majority&appName=Cluster0"
    
    private val client by lazy { 
        try {
            Log.d(TAG, "Initializing MongoClient...")
            val serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .strict(false)
                .build()

            val settings = MongoClientSettings.builder()
                .applyConnectionString(ConnectionString(connectionStringValue))
                .serverApi(serverApi)
                .applyToSslSettings { builder ->
                    builder.enabled(true)
                }
                .applyToClusterSettings { builder ->
                    builder.serverSelectionTimeout(20, TimeUnit.SECONDS)
                }
                .applyToSocketSettings { builder ->
                    builder.connectTimeout(20, TimeUnit.SECONDS)
                    builder.readTimeout(20, TimeUnit.SECONDS)
                }
                .build()
            
            val mClient = MongoClient.create(settings)
            Log.d(TAG, "MongoClient created successfully")
            mClient
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to create MongoClient: ${e.message}", e)
            throw e
        }
    }
    
    private val database by lazy { client.getDatabase("undrift_db") }
    private val usersCollection by lazy { database.getCollection<Document>("users") }

    suspend fun saveUserToMongo(profile: UserProfile): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to save user to MongoDB: ${profile.email}")
            val userDoc = Document()
                .append("name", profile.name)
                .append("email", profile.email)
                .append("password", profile.password)
                .append("age", profile.age)
                .append("goal", profile.goal)
                .append("points", profile.points)
                .append("streakCount", profile.streakCount)
                .append("streakHistory", profile.streakHistory)
                .append("focusDurationMinutes", profile.focusDurationMinutes)
                .append("lastExtraTimePurchaseDate", profile.lastExtraTimePurchaseDate)
                .append("blockedApps", profile.blockedApps.toList())
                .append("updatedAt", System.currentTimeMillis())
            
            val existing = findUserByEmail(profile.email)
            if (existing != null) {
                usersCollection.updateOne(Filters.eq("email", profile.email), Updates.combine(
                    Updates.set("name", profile.name),
                    Updates.set("age", profile.age),
                    Updates.set("goal", profile.goal),
                    Updates.set("points", profile.points),
                    Updates.set("streakCount", profile.streakCount),
                    Updates.set("streakHistory", profile.streakHistory),
                    Updates.set("focusDurationMinutes", profile.focusDurationMinutes),
                    Updates.set("lastExtraTimePurchaseDate", profile.lastExtraTimePurchaseDate),
                    Updates.set("blockedApps", profile.blockedApps.toList()),
                    Updates.set("updatedAt", System.currentTimeMillis())
                ))
            } else {
                userDoc.append("createdAt", System.currentTimeMillis())
                usersCollection.insertOne(userDoc)
            }
            Log.d(TAG, "User saved/updated successfully in MongoDB")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Error saving user to Mongo: ${e.message}", e)
            false
        }
    }

    suspend fun findUserByEmail(email: String): Document? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Finding user by email in MongoDB: $email")
            val result = usersCollection.find(Filters.eq("email", email)).firstOrNull()
            Log.d(TAG, "Find result: ${if (result != null) "User found" else "User not found"}")
            result
        } catch (e: Throwable) {
            Log.e(TAG, "Error finding user in Mongo: ${e.message}", e)
            null
        }
    }

    suspend fun updateUserStats(email: String, points: Int, streakCount: Int, history: List<Int>): Boolean = withContext(Dispatchers.IO) {
        try {
            usersCollection.updateOne(
                Filters.eq("email", email),
                Updates.combine(
                    Updates.set("points", points),
                    Updates.set("streakCount", streakCount),
                    Updates.set("streakHistory", history),
                    Updates.set("updatedAt", System.currentTimeMillis())
                )
            )
            true
        } catch (e: Throwable) {
            false
        }
    }
}
