
@Preview
@Component
export default  struct BackComponent {
  @BuilderParam contentBuilder:()=>void=this.defaultBuilder
  @Builder
  private defaultBuilder(){
    Text("TEST").width(300).height(300)
  }

  build() {
    Stack(){
      Column(){
        this.contentBuilder()
      }.shadow(ShadowStyle.OUTER_DEFAULT_SM).border({radius:10}).backgroundColor(Color.White)

    }.height('100%').width('100%').linearGradient({
      angle:-180,
      colors:[
        [0xf7fbff,0.0],
        [0xdfeefa,1.0]
      ],
      repeating:false
    })
  }
}
