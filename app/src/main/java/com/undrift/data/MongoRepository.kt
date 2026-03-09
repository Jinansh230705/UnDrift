package com.undrift.data

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.bson.Document

class MongoRepository {
    // Standard connection string format compatible with Android
    private val connectionString = "mongodb://main:undrift_starter@ac-1zcpaet-shard-00-00.ey4ivza.mongodb.net:27017,ac-1zcpaet-shard-00-01.ey4ivza.mongodb.net:27017,ac-1zcpaet-shard-00-02.ey4ivza.mongodb.net:27017/?ssl=true&replicaSet=atlas-gaunjv-shard-0&authSource=admin&appName=Cluster0"
    
    // Use lazy to avoid initialization on the Main Thread
    private val client by lazy { MongoClient.create(connectionString) }
    private val database by lazy { client.getDatabase("undrift_db") }
    private val usersCollection by lazy { database.getCollection<Document>("users") }

    suspend fun saveUserToMongo(profile: UserProfile): Boolean = withContext(Dispatchers.IO) {
        try {
            val userDoc = Document()
                .append("name", profile.name)
                .append("email", profile.email)
                .append("age", profile.age)
                .append("goal", profile.goal)
                .append("createdAt", System.currentTimeMillis())
            
            usersCollection.insertOne(userDoc)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun findUserByEmail(email: String): UserProfile? = withContext(Dispatchers.IO) {
        try {
            val doc = usersCollection.find(Filters.eq("email", email)).firstOrNull()
            if (doc != null) {
                UserProfile(
                    name = doc.getString("name") ?: "",
                    email = doc.getString("email") ?: "",
                    age = doc.getString("age") ?: "",
                    goal = doc.getString("goal") ?: "",
                    isLoggedIn = true
                )
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
