import { Context } from "@kit.AbilityKit";
import preferences from "@ohos.data.preferences";


export class prefUtil{
  private static  prefMap:Map<string,preferences.Preferences>=new Map()

  static async init(context:Context,name:string){
    try {
      const pref = await preferences.getPreferences(context,name)
      prefUtil.prefMap.set(name,pref)
      console.info('prefUtil AppTag'+'加载preference成功: '+name)
    }catch (e){
      console.error('prefUtil AppTag'+'加载preference失败: '+name+'ERROR: '+JSON.stringify(e))
    }
  }

  static async put(name:string,key:string,value:preferences.ValueType){
    try {
      const pref = prefUtil.prefMap.get(name)
      if (pref){
        await pref.put(key,value)
        await pref.flush()
        console.info('prefUtil AppTag'+'写入preference成功: '+name+'  Key:'+key+'  Value:'+value)
      }else {
        console.info('prefUtil AppTag'+'preference未初始化: '+name+'  Key:'+key+'  Value:'+value+'ERROR: '+JSON.stringify(pref))
      }
    }catch (e){
      console.error('prefUtil AppTag'+'写入preference失败: '+name+'  Key:'+key+'  Value:'+value+'ERROR: '+JSON.stringify(e))
    }
  }

  static async get (name:string,key:string,defaultValue:preferences.ValueType=''){
    try {
      const pref = prefUtil.prefMap.get(name)
      if (pref) {
        let value = await pref.get(key,defaultValue)
        console.info('prefUtil AppTag'+'读取preference成功: '+name+'  Key:'+key+'  Value:'+value)
        return value
      }else {
        console.info('prefUtil AppTag'+'preference未初始化: '+name+'  Key:'+key+'  Value:'+defaultValue)
        return
      }
    }catch (e){
      console.error('prefUtil AppTag'+'读取preference失败: '+name+'  Key:'+key+'ERROR: '+JSON.stringify(e))
      return
    }
  }

  static async delete(name:string,key:string){
    try {
      const pref = prefUtil.prefMap.get(name)
      if(pref){
        await pref.delete(key)
        console.info('prefUtil AppTag'+'删除preference成功：'+name+'key: '+key)
      }else {
        console.info('prefUtil AppTag'+'preference未初始化: '+name+'  Key:'+key)
      }
    }catch (e){
      console.error('prefUtil AppTag'+'删除preference失败：'+name+'  Key:'+key+'ERROR: '+JSON.stringify(e))
    }
  }

}