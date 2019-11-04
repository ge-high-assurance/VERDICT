(* filename: /Users/a212461047/git/materials/MBAS/tool/examples/modelUnderTest/MBAS-Files/ReadIn/ModelA-1_1-ImplProperty.ml *)

let library = [
	{ name             = "Actuators"; 
	  input_flows      = [ "cmd"; "mstate"; "payloadCmdFC"; "payloadCmdRC"; ]; 
	  output_flows     = [ "battery_level"; "payload_performed"; ]; 
	  faults           = [ ]; 
	  basic_events     = [ ]; 
	  event_info       = [ ]; 
	  fault_formulas   = [ ]; 
	  attacks          = [ "Availability"; "Integrity"; ]; 
	  attack_events    = [ ]; 
	  attack_info      = [ ]; 
	  attack_formulas  = [
		([ "battery_level"; "Availability"; ], Or[ A[ "cmd"; "Availability"; ]; A[ "payloadCmdFC"; "Availability"; ]; A[ "payloadCmdRC"; "Availability"; ]; A[ "mstate"; "Availability"; ]; ]); 
		([ "battery_level"; "Integrity"; ], Or[ A[ "cmd"; "Integrity"; ]; A[ "payloadCmdFC"; "Integrity"; ]; A[ "payloadCmdRC"; "Integrity"; ]; A[ "mstate"; "Integrity"; ]; ]); 
		([ "payload_performed"; "Availability"; ], Or[ A[ "cmd"; "Availability"; ]; A[ "payloadCmdFC"; "Availability"; ]; A[ "payloadCmdRC"; "Availability"; ]; A[ "mstate"; "Availability"; ]; ]); 
		([ "payload_performed"; "Integrity"; ], Or[ A[ "cmd"; "Integrity"; ]; A[ "payloadCmdFC"; "Integrity"; ]; A[ "payloadCmdRC"; "Integrity"; ]; A[ "mstate"; "Integrity"; ]; ]);  ]; 
	  defense_events   = [ ]; 
	  defense_rigors   = [ ]; 
	  defense_profiles = [ ]; 
	}; 
	{ name             = "BatteryHealthCheck"; 
	  input_flows      = [ "battery_level"; ]; 
	  output_flows     = [ "ATE7"; ]; 
	  faults           = [ ]; 
	  basic_events     = [ ]; 
	  event_info       = [ ]; 
	  fault_formulas   = [ ]; 
	  attacks          = [ "Availability"; "Integrity"; ]; 
	  attack_events    = [ ]; 
	  attack_info      = [ ]; 
	  attack_formulas  = [
		([ "ATE7"; "Availability"; ], Or[ A[ "battery_level"; "Availability"; ]; ]); 
		([ "ATE7"; "Integrity"; ], Or[ A[ "battery_level"; "Integrity"; ]; ]);  ]; 
	  defense_events   = [ ]; 
	  defense_rigors   = [ ]; 
	  defense_profiles = [ ]; 
	}; 
	{ name             = "FlightController"; 
	  input_flows      = [ "dest_reached"; "mstate"; "payload_performed"; "tasks"; ]; 
	  output_flows     = [ "payloadCmd"; "waypoint"; ]; 
	  faults           = [ ]; 
	  basic_events     = [ ]; 
	  event_info       = [ ]; 
	  fault_formulas   = [ ]; 
	  attacks          = [ "Availability"; "Integrity"; ]; 
	  attack_events    = [ ]; 
	  attack_info      = [ ]; 
	  attack_formulas  = [
		([ "payloadCmd"; "Availability"; ], Or[ A[ "mstate"; "Availability"; ]; A[ "dest_reached"; "Availability"; ]; A[ "payload_performed"; "Availability"; ]; A[ "tasks"; "Availability"; ]; ]); 
		([ "payloadCmd"; "Integrity"; ], Or[ A[ "mstate"; "Integrity"; ]; A[ "dest_reached"; "Integrity"; ]; A[ "payload_performed"; "Integrity"; ]; A[ "tasks"; "Integrity"; ]; ]); 
		([ "waypoint"; "Availability"; ], Or[ A[ "mstate"; "Availability"; ]; A[ "dest_reached"; "Availability"; ]; A[ "payload_performed"; "Availability"; ]; A[ "tasks"; "Availability"; ]; ]); 
		([ "waypoint"; "Integrity"; ], Or[ A[ "mstate"; "Integrity"; ]; A[ "dest_reached"; "Integrity"; ]; A[ "payload_performed"; "Integrity"; ]; A[ "tasks"; "Integrity"; ]; ]);  ]; 
	  defense_events   = [ ]; 
	  defense_rigors   = [ ]; 
	  defense_profiles = [ ]; 
	}; 
	{ name             = "GPS"; 
	  input_flows      = [ ]; 
	  output_flows     = [ "dir"; "pos"; ]; 
	  faults           = [ ]; 
	  basic_events     = [ ]; 
	  event_info       = [ ]; 
	  fault_formulas   = [ ]; 
	  attacks          = [ "Availability"; "Integrity"; ]; 
	  attack_events    = [ "CAPEC-148"; "CAPEC-601"; ]; 
	  attack_info      = [ 1.000000e+00; 1.000000e+00; ]; 
	  attack_formulas  = [
		([ "dir"; "Availability"; ], Or[ A[ "CAPEC-601"; ]; ]); 
		([ "dir"; "Integrity"; ], Or[ A[ "CAPEC-148"; ]; ]); 
		([ "pos"; "Availability"; ], Or[ A[ "CAPEC-601"; ]; ]); 
		([ "pos"; "Integrity"; ], Or[ A[ "CAPEC-148"; ]; ]);  ]; 
	  defense_events   = [ "antiJamming"; "heterogeneity"; ]; 
	  defense_rigors   = [ 7; 7; ]; 
	  defense_profiles = [
		( "CAPEC-148", And[ D[ "heterogeneity"; ]; ]); 
		( "CAPEC-601", Or[ D[ "antiJamming"; ]; ]);  ]; 
	}; 
	{ name             = "MissionPlanner"; 
	  input_flows      = [ ]; 
	  output_flows     = [ "tasks"; ]; 
	  faults           = [ ]; 
	  basic_events     = [ ]; 
	  event_info       = [ ]; 
	  fault_formulas   = [ ]; 
	  attacks          = [ "Availability"; "Integrity"; ]; 
	  attack_events    = [ ]; 
	  attack_info      = [ ]; 
	  attack_formulas  = [
		([ "tasks"; "Availability"; ], Or[ ]); 
		([ "tasks"; "Integrity"; ], Or[ ]);  ]; 
	  defense_events   = [ ]; 
	  defense_rigors   = [ ]; 
	  defense_profiles = [ ]; 
	}; 
	{ name             = "Mixer"; 
	  input_flows      = [ "currentDir"; "moveNav"; "moveRC"; "mstate"; ]; 
	  output_flows     = [ "cmd"; ]; 
	  faults           = [ ]; 
	  basic_events     = [ ]; 
	  event_info       = [ ]; 
	  fault_formulas   = [ ]; 
	  attacks          = [ "Availability"; "Integrity"; ]; 
	  attack_events    = [ ]; 
	  attack_info      = [ ]; 
	  attack_formulas  = [
		([ "cmd"; "Availability"; ], Or[ A[ "moveNav"; "Availability"; ]; A[ "currentDir"; "Availability"; ]; A[ "mstate"; "Availability"; ]; A[ "moveRC"; "Availability"; ]; ]); 
		([ "cmd"; "Integrity"; ], Or[ A[ "moveNav"; "Integrity"; ]; A[ "currentDir"; "Integrity"; ]; A[ "mstate"; "Integrity"; ]; A[ "moveRC"; "Integrity"; ]; ]);  ]; 
	  defense_events   = [ ]; 
	  defense_rigors   = [ ]; 
	  defense_profiles = [ ]; 
	}; 
	{ name             = "Navigator"; 
	  input_flows      = [ "currentDir"; "currentPos"; "waypoint"; ]; 
	  output_flows     = [ "dest_reached"; "move"; ]; 
	  faults           = [ ]; 
	  basic_events     = [ ]; 
	  event_info       = [ ]; 
	  fault_formulas   = [ ]; 
	  attacks          = [ "Availability"; "Integrity"; ]; 
	  attack_events    = [ ]; 
	  attack_info      = [ ]; 
	  attack_formulas  = [
		([ "dest_reached"; "Availability"; ], Or[ A[ "currentDir"; "Availability"; ]; A[ "waypoint"; "Availability"; ]; A[ "currentPos"; "Availability"; ]; ]); 
		([ "dest_reached"; "Integrity"; ], Or[ A[ "currentDir"; "Integrity"; ]; A[ "waypoint"; "Integrity"; ]; A[ "currentPos"; "Integrity"; ]; ]); 
		([ "move"; "Availability"; ], Or[ A[ "currentDir"; "Availability"; ]; A[ "waypoint"; "Availability"; ]; A[ "currentPos"; "Availability"; ]; ]); 
		([ "move"; "Integrity"; ], Or[ A[ "currentDir"; "Integrity"; ]; A[ "waypoint"; "Integrity"; ]; A[ "currentPos"; "Integrity"; ]; ]);  ]; 
	  defense_events   = [ ]; 
	  defense_rigors   = [ ]; 
	  defense_profiles = [ ]; 
	}; 
	{ name             = "PositionEstimator"; 
	  input_flows      = [ "GPS_dir"; "GPS_pos"; ]; 
	  output_flows     = [ "currentDir"; "currentPos"; ]; 
	  faults           = [ ]; 
	  basic_events     = [ ]; 
	  event_info       = [ ]; 
	  fault_formulas   = [ ]; 
	  attacks          = [ "Availability"; "Integrity"; ]; 
	  attack_events    = [ ]; 
	  attack_info      = [ ]; 
	  attack_formulas  = [
		([ "currentDir"; "Availability"; ], Or[ A[ "GPS_dir"; "Availability"; ]; ]); 
		([ "currentDir"; "Integrity"; ], Or[ A[ "GPS_dir"; "Integrity"; ]; ]); 
		([ "currentPos"; "Availability"; ], Or[ A[ "GPS_pos"; "Availability"; ]; ]); 
		([ "currentPos"; "Integrity"; ], Or[ A[ "GPS_pos"; "Integrity"; ]; ]);  ]; 
	  defense_events   = [ ]; 
	  defense_rigors   = [ ]; 
	  defense_profiles = [ ]; 
	}; 
	{ name             = "RCReceiver"; 
	  input_flows      = [ ]; 
	  output_flows     = [ "move"; "normal"; "payloadCmd"; "uavMode"; ]; 
	  faults           = [ ]; 
	  basic_events     = [ ]; 
	  event_info       = [ ]; 
	  fault_formulas   = [ ]; 
	  attacks          = [ "Availability"; "Integrity"; ]; 
	  attack_events    = [ "CAPEC-125"; "CAPEC-148"; "CAPEC-151"; "CAPEC-28"; "CAPEC-601"; ]; 
	  attack_info      = [ 1.000000e+00; 1.000000e+00; 1.000000e+00; 1.000000e+00; 1.000000e+00; ]; 
	  attack_formulas  = [
		([ "move"; "Availability"; ], Or[ A[ "CAPEC-125"; ]; A[ "CAPEC-601"; ]; ]); 
		([ "move"; "Integrity"; ], Or[ A[ "CAPEC-148"; ]; A[ "CAPEC-151"; ]; A[ "CAPEC-28"; ]; ]); 
		([ "normal"; "Availability"; ], Or[ A[ "CAPEC-125"; ]; A[ "CAPEC-601"; ]; ]); 
		([ "normal"; "Integrity"; ], Or[ A[ "CAPEC-148"; ]; A[ "CAPEC-151"; ]; A[ "CAPEC-28"; ]; ]); 
		([ "payloadCmd"; "Availability"; ], Or[ A[ "CAPEC-125"; ]; A[ "CAPEC-601"; ]; ]); 
		([ "payloadCmd"; "Integrity"; ], Or[ A[ "CAPEC-148"; ]; A[ "CAPEC-151"; ]; A[ "CAPEC-28"; ]; ]); 
		([ "uavMode"; "Availability"; ], Or[ A[ "CAPEC-125"; ]; A[ "CAPEC-601"; ]; ]); 
		([ "uavMode"; "Integrity"; ], Or[ A[ "CAPEC-148"; ]; A[ "CAPEC-151"; ]; A[ "CAPEC-28"; ]; ]);  ]; 
	  defense_events   = [ "antiFlooding"; "antiFuzzing"; "antiJamming"; "encryption"; ]; 
	  defense_rigors   = [ 7; 7; 7; 7; ]; 
	  defense_profiles = [
		( "CAPEC-125", Or[ D[ "antiFlooding"; ]; ]); 
		( "CAPEC-148", And[ D[ "encryption"; ]; ]); 
		( "CAPEC-151", And[ D[ "encryption"; ]; ]); 
		( "CAPEC-28", And[ D[ "antiFuzzing"; ]; ]); 
		( "CAPEC-601", Or[ D[ "antiJamming"; ]; ]);  ]; 
	}; 
	{ name             = "RCReceiverHealthCheck"; 
	  input_flows      = [ "normal"; ]; 
	  output_flows     = [ "ATE6"; ]; 
	  faults           = [ ]; 
	  basic_events     = [ ]; 
	  event_info       = [ ]; 
	  fault_formulas   = [ ]; 
	  attacks          = [ "Availability"; "Integrity"; ]; 
	  attack_events    = [ ]; 
	  attack_info      = [ ]; 
	  attack_formulas  = [
		([ "ATE6"; "Availability"; ], Or[ A[ "normal"; "Availability"; ]; ]); 
		([ "ATE6"; "Integrity"; ], Or[ A[ "normal"; "Integrity"; ]; ]);  ]; 
	  defense_events   = [ ]; 
	  defense_rigors   = [ ]; 
	  defense_profiles = [ ]; 
	}; 
	{ name             = "StateController"; 
	  input_flows      = [ "ATE6"; "ATE7"; "uavMode"; ]; 
	  output_flows     = [ "mstate"; ]; 
	  faults           = [ ]; 
	  basic_events     = [ ]; 
	  event_info       = [ ]; 
	  fault_formulas   = [ ]; 
	  attacks          = [ "Availability"; "Integrity"; ]; 
	  attack_events    = [ ]; 
	  attack_info      = [ ]; 
	  attack_formulas  = [
		([ "mstate"; "Availability"; ], Or[ A[ "ATE6"; "Availability"; ]; A[ "ATE7"; "Availability"; ]; A[ "uavMode"; "Availability"; ]; ]); 
		([ "mstate"; "Integrity"; ], Or[ A[ "ATE6"; "Integrity"; ]; A[ "ATE7"; "Integrity"; ]; A[ "uavMode"; "Integrity"; ]; ]);  ]; 
	  defense_events   = [ ]; 
	  defense_rigors   = [ ]; 
	  defense_profiles = [ ]; 
	}; 
	{ name             = "1_1"; 
	  input_flows      = [ "currentDir"; "currentPos"; "payloadCmd"; ]; 
	  output_flows     = [ "out"; ]; 
	  faults           = [ ]; 
	  basic_events     = [ ]; 
	  event_info       = [ ]; 
	  fault_formulas   = [ ]; 
	  attacks          = [ "Integrity"; "Availability"; ]; 
	  attack_events    = [ ]; 
	  attack_info      = [ ]; 
	  attack_formulas  = [
		([ "out"; "loa"; ], Or[ A[ "payloadCmd"; "Integrity"; ]; A[ "payloadCmd"; "Availability"; ]; A[ "currentPos"; "Integrity"; ]; A[ "currentPos"; "Availability"; ]; A[ "currentDir"; "Integrity"; ]; A[ "currentDir"; "Availability"; ]; ]);  ]; 
	  defense_events   = [ ]; 
	  defense_rigors   = [ ]; 
	  defense_profiles = [ ]; 
	}; 
];;
let model = 
	{ instances = [
		{ i_name = "1_1"; c_name = "1_1"; exposures = [ ]; lambdas = [ ];  }; 
		{ i_name = "RC_receiver"; c_name = "RCReceiver"; exposures = [ ]; lambdas = [ ];  }; 
		{ i_name = "RC_receiver_health_check"; c_name = "RCReceiverHealthCheck"; exposures = [ ]; lambdas = [ ];  }; 
		{ i_name = "actuators"; c_name = "Actuators"; exposures = [ ]; lambdas = [ ];  }; 
		{ i_name = "battery_health_check"; c_name = "BatteryHealthCheck"; exposures = [ ]; lambdas = [ ];  }; 
		{ i_name = "flight_controller"; c_name = "FlightController"; exposures = [ ]; lambdas = [ ];  }; 
		{ i_name = "gps"; c_name = "GPS"; exposures = [ ]; lambdas = [ ];  }; 
		{ i_name = "mission_planner"; c_name = "MissionPlanner"; exposures = [ ]; lambdas = [ ];  }; 
		{ i_name = "mixer"; c_name = "Mixer"; exposures = [ ]; lambdas = [ ];  }; 
		{ i_name = "navigator"; c_name = "Navigator"; exposures = [ ]; lambdas = [ ];  }; 
		{ i_name = "position_estimator"; c_name = "PositionEstimator"; exposures = [ ]; lambdas = [ ];  }; 
		{ i_name = "state_controller"; c_name = "StateController"; exposures = [ ]; lambdas = [ ];  }; 
		]; 
	  connections = [
		(("position_estimator", "GPS_pos"), ("gps", "pos"));
		(("position_estimator", "GPS_dir"), ("gps", "dir"));
		(("navigator", "currentPos"), ("position_estimator", "currentPos"));
		(("navigator", "currentDir"), ("position_estimator", "currentDir"));
		(("mixer", "currentDir"), ("position_estimator", "currentDir"));
		(("flight_controller", "dest_reached"), ("navigator", "dest_reached"));
		(("mixer", "moveNav"), ("navigator", "move"));
		(("navigator", "waypoint"), ("flight_controller", "waypoint"));
		(("actuators", "payloadCmdFC"), ("flight_controller", "payloadCmd"));
		(("flight_controller", "tasks"), ("mission_planner", "tasks"));
		(("RC_receiver_health_check", "normal"), ("RC_receiver", "normal"));
		(("state_controller", "ATE6"), ("RC_receiver_health_check", "ATE6"));
		(("flight_controller", "mstate"), ("state_controller", "mstate"));
		(("mixer", "mstate"), ("state_controller", "mstate"));
		(("flight_controller", "payload_performed"), ("actuators", "payload_performed"));
		(("battery_health_check", "battery_level"), ("actuators", "battery_level"));
		(("state_controller", "ATE7"), ("battery_health_check", "ATE7"));
		(("actuators", "cmd"), ("mixer", "cmd"));
		(("actuators", "payloadCmdRC"), ("RC_receiver", "payloadCmd"));
		(("mixer", "moveRC"), ("RC_receiver", "move"));
		(("state_controller", "uavMode"), ("RC_receiver", "uavMode"));
		(("actuators", "mstate"), ("state_controller", "mstate"));
		(("1_1", "payloadCmd"), ("flight_controller", "payloadCmd"));
		(("1_1", "currentPos"), ("position_estimator", "currentPos"));
		(("1_1", "currentDir"), ("position_estimator", "currentDir"));
		]; 
	  top_fault  = ("", F[ ""; ""; ]); 
	  top_attack = ("1_1", A[ "out"; "loa"; ]); 
	};; 
