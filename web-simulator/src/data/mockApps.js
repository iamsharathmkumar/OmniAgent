export const SAMPLE_COMMANDS = [
  {
    id: "whatsapp",
    title: "Send WhatsApp Message",
    command: "Send a message to Alex on WhatsApp saying I'll be 10m late",
    icon: "MessageSquare",
    color: "#25D366",
    app: "whatsapp",
    steps: [
      {
        step: 1,
        app: "home",
        targetApp: "whatsapp",
        thought: "User wants to send a WhatsApp message to Alex. First, launch WhatsApp from home screen.",
        action: "launch_app",
        targetId: 101,
        targetName: "WhatsApp App Icon",
        coords: { x: 80, y: 310 },
        speech: "Opening WhatsApp..."
      },
      {
        step: 2,
        app: "whatsapp_list",
        targetApp: "whatsapp_chat",
        thought: "In WhatsApp chat list. Found contact 'Alex Rivera' with unread messages. Tap on Alex's conversation.",
        action: "click",
        targetId: 202,
        targetName: "Chat: Alex Rivera",
        coords: { x: 190, y: 220 },
        speech: "Opening chat with Alex..."
      },
      {
        step: 3,
        app: "whatsapp_chat",
        targetApp: "whatsapp_chat",
        thought: "Inside conversation. Tap the message input box and type 'I'll be 10m late'.",
        action: "type",
        targetId: 205,
        targetName: "Message input box",
        textValue: "I'll be 10m late",
        coords: { x: 170, y: 720 },
        speech: "Typing message..."
      },
      {
        step: 4,
        app: "whatsapp_chat",
        targetApp: "whatsapp_chat",
        thought: "Message typed. Tap the green Send button.",
        action: "click",
        targetId: 206,
        targetName: "Send Button",
        coords: { x: 330, y: 720 },
        speech: "Sending message..."
      },
      {
        step: 5,
        app: "whatsapp_chat",
        targetApp: "whatsapp_chat",
        thought: "Message delivered with checkmarks. Goal completed.",
        action: "finish",
        targetName: "Delivered",
        speech: "Done! I've sent the message to Alex on WhatsApp."
      }
    ]
  },
  {
    id: "doordash",
    title: "Order Margherita Pizza",
    command: "Order a Margherita pizza on DoorDash",
    icon: "Utensils",
    color: "#FF3008",
    app: "doordash",
    steps: [
      {
        step: 1,
        app: "home",
        targetApp: "doordash",
        thought: "User wants to order pizza. Launch DoorDash food delivery app.",
        action: "launch_app",
        targetId: 102,
        targetName: "DoorDash App Icon",
        coords: { x: 190, y: 310 },
        speech: "Opening DoorDash..."
      },
      {
        step: 2,
        app: "doordash_home",
        targetApp: "doordash_restaurant",
        thought: "DoorDash opened. Tap on top-rated Italian pizzeria 'Luigi's Woodfire Pizza'.",
        action: "click",
        targetId: 302,
        targetName: "Restaurant: Luigi's Woodfire Pizza",
        coords: { x: 190, y: 310 },
        speech: "Selecting Luigi's Pizzeria..."
      },
      {
        step: 3,
        app: "doordash_restaurant",
        targetApp: "doordash_cart",
        thought: "Found 'Classic Margherita Pizza' ($18.50). Tap 'Add to Cart'.",
        action: "click",
        targetId: 304,
        targetName: "Item: Margherita Pizza (+ Add)",
        coords: { x: 310, y: 390 },
        speech: "Adding Margherita Pizza to cart..."
      },
      {
        step: 4,
        app: "doordash_cart",
        targetApp: "doordash_checkout",
        thought: "Cart has 1 item ($18.50). Financial transaction detected: requesting safety confirmation.",
        action: "ask_confirmation",
        targetName: "Payment Guardrail",
        coords: { x: 190, y: 690 },
        speech: "Margherita pizza is ready to order for $18.50. Shall I authorize payment?"
      },
      {
        step: 5,
        app: "doordash_checkout",
        targetApp: "doordash_checkout",
        thought: "Payment authorized by user. Tapping Place Order button.",
        action: "click",
        targetId: 308,
        targetName: "Place Order ($18.50)",
        coords: { x: 190, y: 690 },
        speech: "Placing your order now..."
      },
      {
        step: 6,
        app: "doordash_checkout",
        targetApp: "doordash_success",
        thought: "Order confirmed! Delivery estimate: 25-30 minutes.",
        action: "finish",
        targetName: "Order Success",
        speech: "Your Margherita pizza has been ordered! It will arrive in about 25 minutes."
      }
    ]
  },
  {
    id: "uber",
    title: "Book Uber to Airport",
    command: "Book a Comfort ride to Central Station on Uber",
    icon: "Car",
    color: "#000000",
    app: "uber",
    steps: [
      {
        step: 1,
        app: "home",
        targetApp: "uber",
        thought: "Voice command to book ride. Launching Uber app.",
        action: "launch_app",
        targetId: 103,
        targetName: "Uber App Icon",
        coords: { x: 300, y: 310 },
        speech: "Opening Uber..."
      },
      {
        step: 2,
        app: "uber_home",
        targetApp: "uber_rides",
        thought: "On Uber home. Entering destination 'Central Station' into 'Where to?' box.",
        action: "type",
        targetId: 402,
        targetName: "'Where to?' Search Input",
        textValue: "Central Station",
        coords: { x: 190, y: 340 },
        speech: "Setting destination to Central Station..."
      },
      {
        step: 3,
        app: "uber_rides",
        targetApp: "uber_rides",
        thought: "Available ride tiers loaded: UberX ($24), Comfort ($31), Black ($48). Select 'Uber Comfort'.",
        action: "click",
        targetId: 405,
        targetName: "Uber Comfort Option ($31.20)",
        coords: { x: 190, y: 530 },
        speech: "Selecting Uber Comfort..."
      },
      {
        step: 4,
        app: "uber_rides",
        targetApp: "uber_confirmed",
        thought: "Tap 'Confirm Comfort' button to finalize pickup.",
        action: "click",
        targetId: 408,
        targetName: "Confirm Comfort Button",
        coords: { x: 190, y: 690 },
        speech: "Confirming Comfort ride..."
      },
      {
        step: 5,
        app: "uber_confirmed",
        targetApp: "uber_confirmed",
        thought: "Driver assigned! Toyota Camry arriving in 4 minutes.",
        action: "finish",
        targetName: "Ride Confirmed",
        speech: "Your Comfort ride is booked! Driver Marcus will arrive in 4 minutes."
      }
    ]
  },
  {
    id: "clock",
    title: "Set Alarm for 7:00 AM",
    command: "Set an alarm for 7:00 AM tomorrow",
    icon: "AlarmClock",
    color: "#3B82F6",
    app: "clock",
    steps: [
      {
        step: 1,
        app: "home",
        targetApp: "clock",
        thought: "User wants to configure an alarm. Launch Clock app.",
        action: "launch_app",
        targetId: 105,
        targetName: "Clock App Icon",
        coords: { x: 80, y: 410 },
        speech: "Opening Clock..."
      },
      {
        step: 2,
        app: "clock_list",
        targetApp: "clock_add",
        thought: "In Clock alarms tab. Tap floating '+' action button to add alarm.",
        action: "click",
        targetId: 502,
        targetName: "Add Alarm Floating Action Button (+)",
        coords: { x: 300, y: 690 },
        speech: "Adding new alarm..."
      },
      {
        step: 3,
        app: "clock_add",
        targetApp: "clock_add",
        thought: "Time picker open. Setting time to 7:00 AM.",
        action: "type",
        targetId: 504,
        targetName: "Alarm Time Picker: 07:00 AM",
        textValue: "07:00",
        coords: { x: 190, y: 420 },
        speech: "Setting time to 7:00 AM..."
      },
      {
        step: 4,
        app: "clock_add",
        targetApp: "clock_list",
        thought: "Tap Save button to persist alarm.",
        action: "click",
        targetId: 508,
        targetName: "Save Alarm Button",
        coords: { x: 310, y: 220 },
        speech: "Saving alarm..."
      },
      {
        step: 5,
        app: "clock_list",
        targetApp: "clock_list",
        thought: "Alarm for 7:00 AM successfully scheduled.",
        action: "finish",
        targetName: "Scheduled",
        speech: "Alarm set for 7:00 AM tomorrow morning."
      }
    ]
  },
  {
    id: "settings",
    title: "Turn On Dark Mode",
    command: "Open Settings and turn on Dark Mode",
    icon: "Moon",
    color: "#6366F1",
    app: "settings",
    steps: [
      {
        step: 1,
        app: "home",
        targetApp: "settings",
        thought: "User requests Dark Mode. Launch Settings app.",
        action: "launch_app",
        targetId: 104,
        targetName: "Settings App Icon",
        coords: { x: 80, y: 510 },
        speech: "Opening Settings..."
      },
      {
        step: 2,
        app: "settings_home",
        targetApp: "settings_home",
        thought: "In Settings. Located 'Display & Brightness' row. Tap to open.",
        action: "click",
        targetId: 603,
        targetName: "Display & Brightness Item",
        coords: { x: 190, y: 340 },
        speech: "Navigating to Display settings..."
      },
      {
        step: 3,
        app: "settings_display",
        targetApp: "settings_display",
        thought: "Found 'Dark Theme' switch (currently OFF). Tap switch to toggle ON.",
        action: "click",
        targetId: 605,
        targetName: "Dark Theme Toggle Switch",
        coords: { x: 310, y: 310 },
        speech: "Enabling Dark Theme..."
      },
      {
        step: 4,
        app: "settings_display",
        targetApp: "settings_display",
        thought: "System theme transitioned to Dark Mode. Goal completed.",
        action: "finish",
        targetName: "Dark Theme Active",
        speech: "Dark Mode is now turned on across your phone."
      }
    ]
  }
];
