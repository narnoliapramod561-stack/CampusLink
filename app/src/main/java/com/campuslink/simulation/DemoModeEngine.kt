package com.campuslink.simulation

import com.campuslink.core.CampusLog
import com.campuslink.data.repository.ChatRepository
import com.campuslink.domain.model.Message
import com.campuslink.domain.model.MessageStatus
import com.campuslink.domain.model.User
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoModeEngine @Inject constructor(
    private val repository: ChatRepository,
    private val scope: CoroutineScope
) {
    private val fakeUsers = listOf(
        User("alice_lpu",   "Alice Sharma",  "AA:BB:CC:DD:EE:01", true, zone="BLOCK_32",    role="STUDENT", department="B.Tech CSE"),
        User("bob_lpu",     "Bob Verma",     "AA:BB:CC:DD:EE:02", true, zone="LIBRARY",     role="STUDENT", department="B.Tech IT"),
        User("charlie_lpu", "Charlie Singh", "AA:BB:CC:DD:EE:03", true, zone="CANTEEN",     role="STUDENT", department="BCA"),
        User("dave_lpu",    "Dave Kapoor",   "AA:BB:CC:DD:EE:04", true, zone="BLOCK_34",    role="FACULTY", department="Professor CSE"),
        User("eve_lpu",     "Eve Kaur",      "AA:BB:CC:DD:EE:05", true, zone="HOSTEL_BOYS", role="ADMIN",   department="Campus Admin")
    )

    suspend fun activate() {
        // Insert all fake users into DB
        fakeUsers.forEach { repository.upsertUser(it) }
        repository.onNodeConnected(); repository.onNodeConnected()
        repository.onNodeConnected(); repository.onNodeConnected()
        CampusLog.d("Demo", "Demo mode activated — 5 virtual LPU users added")
    }

    suspend fun deactivate() {
        fakeUsers.forEach { repository.setUserOnline(it.userId, false) }
    }

    // Simulate sending a message through the relay path with delays
    // myId sends to "eve_lpu", message hops through alice→bob→charlie→dave→eve
    fun simulateRelay(content: String, fromUserId: String, toUserId: String = "eve_lpu") {
        scope.launch {
            val msgId = UUID.randomUUID().toString()
            // Step 1: SENDING
            val msg = Message(
                messageId = msgId, senderId = fromUserId, receiverId = toUserId,
                content = content, timestamp = System.currentTimeMillis(),
                status = MessageStatus.SENDING.name, hopCount = 0,
                pathHistory = Gson().toJson(listOf(fromUserId))
            )
            repository.saveMessage(msg)
            delay(800)
            // Step 2: RELAYED (hop 1)
            repository.updateMessageStatus(msgId, MessageStatus.RELAYED.name)
            repository.onMessageRelayed(1)
            delay(600)
            // Step 3: Relay hop 2
            repository.onMessageRelayed(2)
            delay(600)
            // Step 4: Relay hop 3
            repository.onMessageRelayed(3)
            delay(800)
            // Step 5: DELIVERED
            val deliveredMsg = msg.copy(hopCount = 4)
            repository.updateMessageStatus(msgId, MessageStatus.DELIVERED.name)
            repository.logDelivery(deliveredMsg, success = true)
            CampusLog.d("Demo", "Simulated relay complete: $msgId delivered in 4 hops")
        }
    }

    fun getFakeUsers() = fakeUsers
    fun isActive() = fakeUsers.isNotEmpty()
}
