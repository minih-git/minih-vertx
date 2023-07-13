
<template>
<div>欢迎你！{{ store.state.user.sessionInfo.loginId  }}，{{store.state.user.token}}</div>
</template>

<script setup lang="ts">

import {useStore} from "../store";

const store = useStore()


</script>

<style scoped>






</style>