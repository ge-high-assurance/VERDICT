/*
 * Author: Dan DaCosta
 * Company: Rockwell Collins
 * License: Air Force Open Source Agreement Version 1.0
 *
 * This file has been modified to be used as a simplified example on the CASE project
 * 
 */
 
#pragma once
#include "common/struct_defines.h"
#include "common/conv.h"
#include "Location3D.h"

struct Waypoint_struct {
	
    Location3D super;

    int64_t number;

    int64_t nextwaypoint;

};
typedef struct Waypoint_struct Waypoint;
