import React from 'react';
import { 
  Wifi, 
  Battery, 
  Signal, 
  Mic, 
  Square, 
  Circle, 
  ChevronLeft, 
  Send, 
  Search, 
  Plus, 
  Check, 
  CheckCheck,
  ShoppingBag,
  Clock,
  Car,
  Settings,
  MessageSquare,
  Sparkles,
  MapPin,
  Star,
  ShieldCheck
} from 'lucide-react';

export default function PhoneMockup({
  currentApp,
  setCurrentApp,
  agentState,
  currentThought,
  activeTap,
  showInspector,
  typedText,
  isDarkMode,
  setIsDarkMode,
  onEmergencyStop,
  onStartListening
}) {
  return (
    <div className="relative mx-auto flex flex-col items-center">
      {/* Outer Phone Hardware Bezel */}
      <div className="relative w-[340px] h-[700px] bg-slate-900 rounded-[48px] p-[10px] shadow-2xl border-4 border-slate-700/80 ring-1 ring-white/10 select-none">
        {/* Speaker ear slit */}
        <div className="absolute top-3 left-1/2 -translate-x-1/2 w-16 h-1.5 bg-slate-800 rounded-full z-50"></div>

        {/* Screen Bezel Container */}
        <div className={`relative w-full h-full rounded-[38px] overflow-hidden flex flex-col transition-colors duration-300 ${
          isDarkMode ? 'bg-[#0B0F17] text-white' : 'bg-slate-50 text-slate-900'
        }`}>
          
          {/* Status Bar */}
          <div className="h-10 px-6 pt-2 flex items-center justify-between text-xs font-semibold z-40">
            <span>9:41</span>
            
            {/* Dynamic Island / Floating Notch */}
            <div className="w-24 h-5 bg-black rounded-full flex items-center justify-center space-x-1.5 px-2">
              <div className="w-2.5 h-2.5 rounded-full bg-slate-900 border border-slate-800 flex items-center justify-center">
                <div className="w-1 h-1 rounded-full bg-blue-900"></div>
              </div>
              {agentState === 'THINKING' || agentState === 'EXECUTING' ? (
                <div className="flex items-center space-x-1">
                  <span className="w-1.5 h-1.5 rounded-full bg-indigo-400 animate-ping"></span>
                  <span className="text-[9px] text-indigo-300 font-mono tracking-tighter">AI ACT</span>
                </div>
              ) : null}
            </div>

            <div className="flex items-center space-x-1.5 text-slate-400">
              <Signal className="w-3.5 h-3.5" />
              <Wifi className="w-3.5 h-3.5" />
              <Battery className="w-4 h-4 text-emerald-400" />
            </div>
          </div>

          {/* Floating OmniAgent Dynamic Island / HUD Overlay */}
          <div className="absolute top-12 left-4 right-4 z-40 transition-all duration-300">
            <div className="bg-slate-900/95 backdrop-blur-md border border-indigo-500/40 shadow-xl rounded-2xl p-2.5 flex items-center justify-between text-white">
              <div className="flex items-center space-x-2 overflow-hidden">
                <button 
                  onClick={onStartListening}
                  className={`w-8 h-8 rounded-xl flex items-center justify-center transition-all ${
                    agentState === 'LISTENING' 
                      ? 'bg-rose-500 text-white animate-pulse' 
                      : agentState === 'EXECUTING'
                      ? 'bg-indigo-600 text-white'
                      : 'bg-indigo-500/20 text-indigo-400 hover:bg-indigo-500/30'
                  }`}
                >
                  <Mic className="w-4 h-4" />
                </button>
                <div className="flex flex-col min-w-0 pr-2">
                  <div className="flex items-center space-x-1.5">
                    <span className="text-[10px] font-bold tracking-wider text-indigo-400 uppercase">
                      {agentState}
                    </span>
                    {agentState === 'EXECUTING' && (
                      <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-ping"></span>
                    )}
                  </div>
                  <span className="text-xs text-slate-200 truncate font-medium">
                    {currentThought || "Say 'Hey Omni' or tap mic"}
                  </span>
                </div>
              </div>

              {(agentState === 'EXECUTING' || agentState === 'THINKING') && (
                <button
                  onClick={onEmergencyStop}
                  title="Emergency Stop"
                  className="px-2 py-1 bg-rose-500/20 hover:bg-rose-500/40 text-rose-300 border border-rose-500/40 rounded-lg text-[10px] font-bold flex items-center space-x-1 shrink-0"
                >
                  <Square className="w-2.5 h-2.5 fill-current" />
                  <span>HALT</span>
                </button>
              )}
            </div>
          </div>

          {/* Screen Content Body */}
          <div className="flex-1 relative overflow-y-auto overflow-x-hidden pt-12 pb-12">
            
            {/* 1. HOME SCREEN */}
            {currentApp === 'home' && (
              <div className="p-4 h-full flex flex-col justify-between">
                <div>
                  {/* Google Search Bar Widget */}
                  <div className="bg-slate-800/80 rounded-2xl p-3 flex items-center justify-between text-slate-400 shadow-sm border border-slate-700/50 mt-2 mb-6">
                    <span className="text-xs text-slate-300 font-medium">Search Google or say commands...</span>
                    <Mic className="w-4 h-4 text-indigo-400" />
                  </div>

                  {/* App Grid */}
                  <div className="grid grid-cols-4 gap-4 text-center">
                    <AppIcon 
                      id={101}
                      name="WhatsApp" 
                      bg="bg-emerald-600" 
                      icon={<MessageSquare className="w-6 h-6 text-white" />}
                      onClick={() => setCurrentApp('whatsapp_list')}
                      showInspector={showInspector}
                    />
                    <AppIcon 
                      id={102}
                      name="DoorDash" 
                      bg="bg-rose-600" 
                      icon={<ShoppingBag className="w-6 h-6 text-white" />}
                      onClick={() => setCurrentApp('doordash_home')}
                      showInspector={showInspector}
                    />
                    <AppIcon 
                      id={103}
                      name="Uber" 
                      bg="bg-black border border-slate-700" 
                      icon={<Car className="w-6 h-6 text-white" />}
                      onClick={() => setCurrentApp('uber_home')}
                      showInspector={showInspector}
                    />
                    <AppIcon 
                      id={104}
                      name="Settings" 
                      bg="bg-slate-700" 
                      icon={<Settings className="w-6 h-6 text-white" />}
                      onClick={() => setCurrentApp('settings_home')}
                      showInspector={showInspector}
                    />
                    <AppIcon 
                      id={105}
                      name="Clock" 
                      bg="bg-blue-600" 
                      icon={<Clock className="w-6 h-6 text-white" />}
                      onClick={() => setCurrentApp('clock_list')}
                      showInspector={showInspector}
                    />
                    <AppIcon 
                      id={106}
                      name="Camera" 
                      bg="bg-amber-600" 
                      icon={<Sparkles className="w-6 h-6 text-white" />}
                      onClick={() => {}}
                      showInspector={showInspector}
                    />
                  </div>
                </div>

                {/* Dock */}
                <div className="bg-slate-800/50 backdrop-blur-md rounded-3xl p-3 flex justify-around border border-slate-700/40">
                  <div className="w-10 h-10 rounded-2xl bg-emerald-500 flex items-center justify-center text-white">
                    <MessageSquare className="w-5 h-5" />
                  </div>
                  <div className="w-10 h-10 rounded-2xl bg-blue-500 flex items-center justify-center text-white">
                    <Search className="w-5 h-5" />
                  </div>
                  <div className="w-10 h-10 rounded-2xl bg-slate-700 flex items-center justify-center text-white">
                    <Settings className="w-5 h-5" />
                  </div>
                </div>
              </div>
            )}

            {/* 2. WHATSAPP CHAT LIST */}
            {currentApp === 'whatsapp_list' && (
              <div className="h-full flex flex-col bg-[#111B21] text-white">
                <div className="p-3 bg-[#202C33] flex items-center justify-between">
                  <span className="font-bold text-emerald-400">WhatsApp</span>
                  <div className="flex space-x-3 text-slate-400">
                    <Search className="w-4 h-4" />
                  </div>
                </div>

                <div className="divide-y divide-slate-800 flex-1">
                  <div 
                    onClick={() => setCurrentApp('whatsapp_chat')}
                    className="p-3 flex items-center space-x-3 hover:bg-slate-800/60 cursor-pointer relative"
                  >
                    {showInspector && <InspectorBadge id={202} />}
                    <div className="w-11 h-11 rounded-full bg-gradient-to-tr from-amber-500 to-rose-500 flex items-center justify-center font-bold text-white shrink-0">
                      AR
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex justify-between items-baseline">
                        <span className="font-semibold text-sm">Alex Rivera</span>
                        <span className="text-[10px] text-slate-400">9:35 AM</span>
                      </div>
                      <p className="text-xs text-slate-400 truncate">Are you almost here for the meeting?</p>
                    </div>
                  </div>

                  <div className="p-3 flex items-center space-x-3 hover:bg-slate-800/60 cursor-pointer">
                    <div className="w-11 h-11 rounded-full bg-gradient-to-tr from-purple-500 to-indigo-500 flex items-center justify-center font-bold text-white shrink-0">
                      M
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex justify-between items-baseline">
                        <span className="font-semibold text-sm">Mom</span>
                        <span className="text-[10px] text-slate-400">Yesterday</span>
                      </div>
                      <p className="text-xs text-slate-400 truncate">Call me when you're free ❤️</p>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* 3. WHATSAPP CONVERSATION */}
            {currentApp === 'whatsapp_chat' && (
              <div className="h-full flex flex-col bg-[#0B141A] text-white justify-between">
                {/* Header */}
                <div className="p-2.5 bg-[#202C33] flex items-center space-x-2">
                  <button onClick={() => setCurrentApp('whatsapp_list')}>
                    <ChevronLeft className="w-5 h-5 text-slate-300" />
                  </button>
                  <div className="w-8 h-8 rounded-full bg-amber-500 flex items-center justify-center text-xs font-bold">
                    AR
                  </div>
                  <div>
                    <div className="text-xs font-bold">Alex Rivera</div>
                    <div className="text-[10px] text-emerald-400">Online</div>
                  </div>
                </div>

                {/* Messages Body */}
                <div className="p-3 space-y-2 flex-1 overflow-y-auto">
                  <div className="bg-[#202C33] p-2.5 rounded-xl rounded-tl-none max-w-[80%] text-xs text-slate-200 shadow-sm">
                    Are you almost here for the meeting?
                    <div className="text-[9px] text-slate-400 text-right mt-1">9:35 AM</div>
                  </div>

                  {typedText && (
                    <div className="bg-[#005C4B] p-2.5 rounded-xl rounded-tr-none max-w-[80%] ml-auto text-xs text-white shadow-sm flex items-end justify-between space-x-2">
                      <span>{typedText}</span>
                      <div className="flex items-center space-x-1 shrink-0">
                        <span className="text-[9px] text-emerald-200">9:41 AM</span>
                        <CheckCheck className="w-3 h-3 text-cyan-300" />
                      </div>
                    </div>
                  )}
                </div>

                {/* Input Bar */}
                <div className="p-2 bg-[#202C33] flex items-center space-x-2 relative">
                  {showInspector && <InspectorBadge id={205} />}
                  <input
                    type="text"
                    value={typedText}
                    readOnly
                    placeholder="Type a message..."
                    className="flex-1 bg-[#2A3942] rounded-full px-4 py-2 text-xs text-white focus:outline-none"
                  />
                  <div className="relative">
                    {showInspector && <InspectorBadge id={206} />}
                    <button className="w-9 h-9 rounded-full bg-[#00A884] flex items-center justify-center text-white shadow-md">
                      <Send className="w-4 h-4 ml-0.5" />
                    </button>
                  </div>
                </div>
              </div>
            )}

            {/* 4. DOORDASH */}
            {(currentApp === 'doordash_home' || currentApp === 'doordash_restaurant' || currentApp === 'doordash_cart' || currentApp === 'doordash_checkout' || currentApp === 'doordash_success') && (
              <div className="h-full flex flex-col bg-slate-900 text-white">
                <div className="p-3 bg-slate-800 flex items-center justify-between border-b border-slate-700">
                  <div className="flex items-center space-x-1">
                    <span className="font-extrabold text-rose-500 tracking-tight text-sm">DOORDASH</span>
                  </div>
                  <div className="text-xs text-slate-400 flex items-center space-x-1">
                    <MapPin className="w-3 h-3 text-rose-400" />
                    <span>Home (25-35m)</span>
                  </div>
                </div>

                {currentApp === 'doordash_home' && (
                  <div className="p-3 space-y-3">
                    <div className="text-xs font-bold text-slate-300">Popular Near You</div>
                    <div 
                      onClick={() => setCurrentApp('doordash_restaurant')}
                      className="bg-slate-800 rounded-xl p-3 border border-slate-700 cursor-pointer relative hover:border-rose-500/50"
                    >
                      {showInspector && <InspectorBadge id={302} />}
                      <div className="h-20 bg-gradient-to-r from-amber-700 to-rose-900 rounded-lg mb-2 flex items-center justify-center text-2xl">
                        🍕
                      </div>
                      <div className="flex justify-between items-center">
                        <span className="font-bold text-sm">Luigi's Woodfire Pizza</span>
                        <div className="flex items-center text-xs text-amber-400 font-semibold">
                          <Star className="w-3 h-3 fill-current mr-0.5" /> 4.9
                        </div>
                      </div>
                      <div className="text-xs text-slate-400">Italian • Artisan Pizza • $0.99 delivery</div>
                    </div>
                  </div>
                )}

                {currentApp === 'doordash_restaurant' && (
                  <div className="p-3 space-y-3">
                    <div className="flex items-center space-x-2">
                      <button onClick={() => setCurrentApp('doordash_home')}>
                        <ChevronLeft className="w-4 h-4 text-slate-400" />
                      </button>
                      <span className="font-bold text-sm">Luigi's Woodfire Pizza</span>
                    </div>

                    <div className="bg-slate-800 rounded-xl p-3 border border-slate-700 relative">
                      {showInspector && <InspectorBadge id={304} />}
                      <div className="flex justify-between items-start">
                        <div>
                          <div className="font-bold text-sm">Classic Margherita Pizza</div>
                          <div className="text-xs text-slate-400 mt-0.5">San Marzano tomatoes, fresh mozzarella, basil & EVOO</div>
                          <div className="text-sm font-bold text-emerald-400 mt-2">$18.50</div>
                        </div>
                        <button 
                          onClick={() => setCurrentApp('doordash_cart')}
                          className="px-3 py-1.5 bg-rose-600 hover:bg-rose-500 text-white rounded-lg text-xs font-bold shrink-0 ml-2 shadow-md"
                        >
                          + Add
                        </button>
                      </div>
                    </div>
                  </div>
                )}

                {(currentApp === 'doordash_cart' || currentApp === 'doordash_checkout') && (
                  <div className="p-3 flex-1 flex flex-col justify-between">
                    <div>
                      <div className="text-xs font-bold text-slate-400 mb-2">CART (1 ITEM)</div>
                      <div className="bg-slate-800 rounded-xl p-3 border border-slate-700 flex justify-between items-center">
                        <div>
                          <div className="text-sm font-bold">1x Classic Margherita Pizza</div>
                          <div className="text-xs text-slate-400">Extra fresh basil</div>
                        </div>
                        <div className="font-bold text-sm">$18.50</div>
                      </div>

                      <div className="mt-4 p-3 bg-slate-800/60 rounded-xl border border-slate-700/60 space-y-1.5 text-xs text-slate-300">
                        <div className="flex justify-between"><span>Subtotal</span><span>$18.50</span></div>
                        <div className="flex justify-between"><span>Delivery</span><span>$0.99</span></div>
                        <div className="flex justify-between font-bold text-white pt-2 border-t border-slate-700">
                          <span>Total</span>
                          <span className="text-emerald-400">$19.49</span>
                        </div>
                      </div>
                    </div>

                    <div className="relative">
                      {showInspector && <InspectorBadge id={308} />}
                      <button 
                        onClick={() => setCurrentApp('doordash_success')}
                        className="w-full py-3 bg-rose-600 hover:bg-rose-500 font-bold text-sm rounded-xl text-white shadow-lg flex items-center justify-center space-x-2"
                      >
                        <ShieldCheck className="w-4 h-4" />
                        <span>Place Order ($19.49)</span>
                      </button>
                    </div>
                  </div>
                )}

                {currentApp === 'doordash_success' && (
                  <div className="p-6 flex-1 flex flex-col items-center justify-center text-center space-y-3">
                    <div className="w-16 h-16 rounded-full bg-emerald-500/20 border-2 border-emerald-500 flex items-center justify-center text-emerald-400 text-2xl animate-bounce">
                      ✓
                    </div>
                    <div className="font-bold text-lg">Order Confirmed!</div>
                    <div className="text-xs text-slate-400">Luigi's Woodfire Pizza is preparing your Margherita pizza.</div>
                    <div className="text-sm font-bold text-emerald-400">Est. Arrival: 25 mins</div>
                  </div>
                )}
              </div>
            )}

            {/* 5. UBER */}
            {(currentApp === 'uber_home' || currentApp === 'uber_rides' || currentApp === 'uber_confirmed') && (
              <div className="h-full flex flex-col bg-slate-950 text-white">
                <div className="p-3 bg-black flex items-center justify-between border-b border-slate-800">
                  <span className="font-black text-base tracking-widest">Uber</span>
                  <div className="w-6 h-6 rounded-full bg-slate-800"></div>
                </div>

                {currentApp === 'uber_home' && (
                  <div className="p-3 space-y-4">
                    <div className="h-28 bg-gradient-to-br from-indigo-950 to-slate-900 rounded-xl border border-slate-800 p-3 flex flex-col justify-end">
                      <span className="text-xs font-semibold text-indigo-300">Where are you heading?</span>
                    </div>

                    <div className="relative">
                      {showInspector && <InspectorBadge id={402} />}
                      <div 
                        onClick={() => setCurrentApp('uber_rides')}
                        className="bg-slate-900 border border-slate-700 rounded-xl p-3 flex items-center space-x-2 cursor-pointer"
                      >
                        <Search className="w-4 h-4 text-slate-400" />
                        <span className="text-xs text-slate-300">Where to?</span>
                      </div>
                    </div>
                  </div>
                )}

                {currentApp === 'uber_rides' && (
                  <div className="p-3 flex-1 flex flex-col justify-between">
                    <div className="space-y-2">
                      <div className="text-xs font-bold text-slate-400">CHOOSE A RIDE</div>

                      <div className="p-2.5 bg-slate-900 rounded-xl border border-slate-800 flex justify-between items-center">
                        <div className="flex items-center space-x-2">
                          <Car className="w-5 h-5 text-slate-300" />
                          <div>
                            <div className="text-xs font-bold">UberX</div>
                            <div className="text-[10px] text-slate-400">3 min away • 4 seats</div>
                          </div>
                        </div>
                        <span className="font-bold text-xs">$24.10</span>
                      </div>

                      <div className="p-2.5 bg-indigo-950/60 rounded-xl border-2 border-indigo-500 flex justify-between items-center relative">
                        {showInspector && <InspectorBadge id={405} />}
                        <div className="flex items-center space-x-2">
                          <Car className="w-5 h-5 text-indigo-400" />
                          <div>
                            <div className="text-xs font-bold text-indigo-200">Uber Comfort</div>
                            <div className="text-[10px] text-indigo-300">4 min away • Extra legroom</div>
                          </div>
                        </div>
                        <span className="font-bold text-xs text-indigo-300">$31.20</span>
                      </div>
                    </div>

                    <div className="relative">
                      {showInspector && <InspectorBadge id={408} />}
                      <button 
                        onClick={() => setCurrentApp('uber_confirmed')}
                        className="w-full py-3 bg-white text-black font-extrabold text-sm rounded-xl hover:bg-slate-200 transition-colors"
                      >
                        Confirm Comfort ($31.20)
                      </button>
                    </div>
                  </div>
                )}

                {currentApp === 'uber_confirmed' && (
                  <div className="p-6 flex-1 flex flex-col items-center justify-center text-center space-y-3">
                    <div className="w-16 h-16 rounded-full bg-indigo-600/20 border-2 border-indigo-500 flex items-center justify-center text-indigo-400 text-2xl animate-pulse">
                      🚗
                    </div>
                    <div className="font-bold text-base">Driver Marcus Assigned!</div>
                    <div className="text-xs text-slate-400">Toyota Camry (Black) • Plate 7XY92</div>
                    <div className="text-xs font-bold text-emerald-400">Arriving in 4 minutes</div>
                  </div>
                )}
              </div>
            )}

            {/* 6. CLOCK / ALARMS */}
            {(currentApp === 'clock_list' || currentApp === 'clock_add') && (
              <div className="h-full flex flex-col bg-slate-900 text-white justify-between">
                <div>
                  <div className="p-3 bg-slate-800 flex items-center justify-between border-b border-slate-700">
                    <span className="font-bold text-sm">Alarms</span>
                    <Clock className="w-4 h-4 text-blue-400" />
                  </div>

                  <div className="p-3 space-y-3">
                    <div className="bg-slate-800 rounded-xl p-3 border border-slate-700 flex justify-between items-center">
                      <div>
                        <div className="text-2xl font-bold">07:00 <span className="text-xs font-normal text-slate-400">AM</span></div>
                        <div className="text-[11px] text-blue-400">Tomorrow - Weekdays</div>
                      </div>
                      <div className="w-10 h-5 bg-blue-600 rounded-full p-0.5 flex justify-end">
                        <div className="w-4 h-4 rounded-full bg-white"></div>
                      </div>
                    </div>

                    <div className="bg-slate-800 rounded-xl p-3 border border-slate-700 flex justify-between items-center opacity-60">
                      <div>
                        <div className="text-2xl font-bold">08:30 <span className="text-xs font-normal text-slate-400">AM</span></div>
                        <div className="text-[11px] text-slate-400">Weekend</div>
                      </div>
                      <div className="w-10 h-5 bg-slate-600 rounded-full p-0.5 flex justify-start">
                        <div className="w-4 h-4 rounded-full bg-slate-400"></div>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="p-4 flex justify-center relative">
                  {showInspector && <InspectorBadge id={502} />}
                  <button 
                    onClick={() => setCurrentApp('clock_add')}
                    className="w-12 h-12 rounded-full bg-blue-600 hover:bg-blue-500 flex items-center justify-center text-white shadow-lg"
                  >
                    <Plus className="w-6 h-6" />
                  </button>
                </div>
              </div>
            )}

            {/* 7. SETTINGS */}
            {(currentApp === 'settings_home' || currentApp === 'settings_display') && (
              <div className={`h-full flex flex-col ${isDarkMode ? 'bg-slate-900 text-white' : 'bg-slate-100 text-slate-900'}`}>
                <div className={`p-3 border-b flex items-center justify-between ${isDarkMode ? 'bg-slate-800 border-slate-700' : 'bg-white border-slate-200'}`}>
                  <span className="font-bold text-sm">Settings</span>
                  <Settings className="w-4 h-4" />
                </div>

                <div className="p-3 space-y-2">
                  <div 
                    onClick={() => setCurrentApp('settings_display')}
                    className={`p-3 rounded-xl border flex justify-between items-center cursor-pointer relative ${
                      isDarkMode ? 'bg-slate-800 border-slate-700' : 'bg-white border-slate-200'
                    }`}
                  >
                    {showInspector && <InspectorBadge id={603} />}
                    <div className="flex items-center space-x-2">
                      <div className="w-7 h-7 rounded-lg bg-indigo-500/20 text-indigo-400 flex items-center justify-center">
                        <Sparkles className="w-4 h-4" />
                      </div>
                      <span className="text-xs font-semibold">Display & Brightness</span>
                    </div>
                    <ChevronLeft className="w-4 h-4 rotate-180 text-slate-400" />
                  </div>

                  {currentApp === 'settings_display' && (
                    <div className={`p-3 rounded-xl border mt-3 flex justify-between items-center relative ${
                      isDarkMode ? 'bg-slate-800 border-slate-700' : 'bg-white border-slate-200'
                    }`}>
                      {showInspector && <InspectorBadge id={605} />}
                      <div>
                        <div className="text-xs font-bold">Dark Theme</div>
                        <div className="text-[10px] text-slate-400">Saves battery and eye comfort</div>
                      </div>
                      <button 
                        onClick={() => setIsDarkMode(!isDarkMode)}
                        className={`w-11 h-6 rounded-full p-0.5 transition-colors ${
                          isDarkMode ? 'bg-indigo-600 flex justify-end' : 'bg-slate-400 flex justify-start'
                        }`}
                      >
                        <div className="w-5 h-5 rounded-full bg-white shadow-sm"></div>
                      </button>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Virtual Finger Tap Ripple Indicator */}
            {activeTap && (
              <div 
                className="absolute z-50 pointer-events-none transition-all duration-300"
                style={{ left: `${activeTap.x}px`, top: `${activeTap.y}px` }}
              >
                <div className="relative -translate-x-1/2 -translate-y-1/2">
                  {/* Outer pulse */}
                  <span className="absolute w-10 h-10 rounded-full bg-indigo-400/40 animate-ping"></span>
                  {/* Inner touch dot */}
                  <div className="w-6 h-6 rounded-full bg-indigo-500/90 border-2 border-white shadow-lg flex items-center justify-center text-[8px] text-white font-bold">
                    TAP
                  </div>
                </div>
              </div>
            )}

          </div>

          {/* Android Navigation Bar */}
          <div className="h-10 bg-slate-950 flex items-center justify-around text-slate-400 border-t border-slate-800/60 z-40">
            <button 
              onClick={() => {
                if (currentApp === 'whatsapp_chat') setCurrentApp('whatsapp_list');
                else if (currentApp === 'whatsapp_list') setCurrentApp('home');
                else if (currentApp === 'doordash_restaurant') setCurrentApp('doordash_home');
                else if (currentApp === 'doordash_cart') setCurrentApp('doordash_restaurant');
                else setCurrentApp('home');
              }}
              title="Back"
              className="p-1 hover:text-white"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <button 
              onClick={() => setCurrentApp('home')}
              title="Home"
              className="p-1 hover:text-white"
            >
              <Circle className="w-3.5 h-3.5" />
            </button>
            <button 
              title="Recents"
              className="p-1 hover:text-white"
            >
              <Square className="w-3 h-3" />
            </button>
          </div>

        </div>
      </div>
    </div>
  );
}

function AppIcon({ id, name, bg, icon, onClick, showInspector }) {
  return (
    <div className="flex flex-col items-center space-y-1 relative">
      {showInspector && <InspectorBadge id={id} />}
      <button 
        onClick={onClick}
        className={`w-14 h-14 rounded-2xl ${bg} flex items-center justify-center shadow-md active:scale-95 transition-transform hover:ring-2 hover:ring-indigo-400`}
      >
        {icon}
      </button>
      <span className="text-[11px] font-medium text-slate-200">{name}</span>
    </div>
  );
}

function InspectorBadge({ id }) {
  return (
    <span className="absolute -top-1 -right-1 z-30 bg-rose-500 text-white font-mono text-[9px] px-1 py-0.2 rounded font-bold shadow-md border border-white">
      [{id}]
    </span>
  );
}
