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

struct Location3D_struct {

// Units: degree
    uint64_t latitude;

// Units: degree
    uint64_t longitude;

// Units: meter
    uint32_t altitude;

};
typedef struct Location3D_struct Location3D;
